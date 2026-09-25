package com.youlai.boot.module.form.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.common.exception.BusinessException;
import com.youlai.boot.common.model.KeyValue;
import com.youlai.boot.common.model.Option;
import com.youlai.boot.module.form.converter.FormConverter;
import com.youlai.boot.module.form.enums.FormCategoryEnum;
import com.youlai.boot.module.form.enums.FormStatusEnum;
import com.youlai.boot.module.form.mapper.FormDataMapper;
import com.youlai.boot.module.form.mapper.FormDefinitionMapper;
import com.youlai.boot.module.form.mapper.FormSnapshotMapper;
import com.youlai.boot.module.form.model.entity.FormData;
import com.youlai.boot.module.form.model.entity.FormDefinition;
import com.youlai.boot.module.form.model.entity.FormSnapshot;
import com.youlai.boot.module.form.model.form.FormDefinitionForm;
import com.youlai.boot.module.form.model.form.FormMenuForm;
import com.youlai.boot.module.form.model.query.FormDefinitionPageQuery;
import com.youlai.boot.module.form.model.vo.FormDefinitionPageVO;
import com.youlai.boot.module.form.model.vo.FormMenuVO;
import com.youlai.boot.module.form.model.vo.FormRenderVO;
import com.youlai.boot.module.form.service.FormDefinitionService;
import com.youlai.boot.module.form.util.FormRuleValidator;
import com.youlai.boot.module.workflow.service.WorkflowModelService;
import com.youlai.boot.support.ai.AiChatService;
import com.youlai.boot.support.ai.PromptLoader;
import com.youlai.boot.system.enums.MenuTypeEnum;
import com.youlai.boot.system.model.entity.Menu;
import com.youlai.boot.system.model.entity.RoleMenu;
import com.youlai.boot.system.model.form.MenuForm;
import com.youlai.boot.system.service.MenuService;
import com.youlai.boot.system.service.RoleMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 表单定义业务实现类
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@Service
@RequiredArgsConstructor
public class FormDefinitionServiceImpl extends ServiceImpl<FormDefinitionMapper, FormDefinition> implements FormDefinitionService {

    /** 系统提示词名称，对应 resources/prompts 下的相对路径 */
    private static final String SYSTEM_PROMPT_PATH = "form/system.md";

    private final FormConverter formConverter;
    private final RedisTemplate<String, Object> redisTemplate;
    private final MenuService menuService;
    private final RoleMenuService roleMenuService;
    private final FormSnapshotMapper formSnapshotMapper;
    private final FormDataMapper formDataMapper;
    private final ObjectProvider<WorkflowModelService> workflowModelServiceProvider;
    private final AiChatService aiChatService;
    private final PromptLoader promptLoader;

    /** 默认挂载目录名称：发布向导未显式选目录时自动使用/创建 */
    private static final String DEFAULT_CATALOG_NAME = "表单中心";

    /** 表单管理目录名称（顶级，dynamic-form.sql 预置）："表单中心"默认挂载其下，顶级入口聚合 */
    private static final String FORM_ADMIN_CATALOG_NAME = "动态表单";

    /** 渲染缓存兜底 TTL：事件驱动清缓存外再设过期，防 DB 更新与删缓存间应用重启导致脏缓存永续 */
    private static final long RENDER_CACHE_TTL_MINUTES = 30;

    /**
     * 表单定义分页列表
     *
     * @param queryParams 查询参数
     */
    @Override
    public Page<FormDefinitionPageVO> getFormDefinitionPage(FormDefinitionPageQuery queryParams) {
        int pageNum = queryParams.getPageNum();
        int pageSize = queryParams.getPageSize();
        return this.baseMapper.getFormDefinitionPage(new Page<>(pageNum, pageSize), queryParams);
    }

    /**
     * 按主键查询表单定义
     *
     * @param id 表单ID
     */
    @Override
    public FormDefinition getFormDefinition(Long id) {
        return this.getById(id);
    }

    /**
     * 获取表单定义表单数据
     *
     * @param id 表单ID
     */
    @Override
    public FormDefinitionForm getFormDefinitionForm(Long id) {
        FormDefinition entity = this.getById(id);
        if (entity == null) {
            throw new BusinessException("表单不存在");
        }
        return formConverter.toForm(entity);
    }

    /**
     * 新增表单定义
     *
     * @param formData 表单定义表单数据
     */
    @Override
    public boolean saveFormDefinition(FormDefinitionForm formData) {
        // 校验 formKey 唯一（逻辑删除查询默认过滤，同 key 删除后可重建）
        String formKey = formData.getFormKey();
        long count = this.count(new LambdaQueryWrapper<FormDefinition>()
                .eq(FormDefinition::getFormKey, formKey)
        );
        Assert.isTrue(count == 0, "表单标识已存在");

        FormDefinition entity = formConverter.toEntity(formData);
        // 新表单初始为草稿态；规则未设计时空数组占位，设计器打开为空白画布
        entity.setStatus(FormStatusEnum.DRAFT.getValue());
        entity.setVersion(1);
        // 类型缺省为普通表单，须显式选择审批表单才可被工作流绑定
        if (StrUtil.isBlank(entity.getCategory())) {
            entity.setCategory(FormCategoryEnum.NORMAL.getValue());
        }
        if (entity.getFormJson() == null) {
            entity.setFormJson("[]");
        }
        return this.save(entity);
    }

    /**
     * 修改表单定义
     *
     * <p>formKey 创建后不可修改；草稿态可修改元数据与规则，非草稿态仅可修改规则</p>
     *
     * @param id       表单ID
     * @param formData 表单定义表单数据
     */
    @Override
    public boolean updateFormDefinition(Long id, FormDefinitionForm formData) {
        FormDefinition entity = this.getById(id);
        if (entity == null) {
            throw new BusinessException("表单不存在");
        }
        // formKey 为 null 表示部分更新未携带该字段（如设计器仅保存规则），不视为修改；
        // 显式传入不同标识才拒绝，防止已发布表单的业务编码被越权变更
        if (formData.getFormKey() != null && !formData.getFormKey().equals(entity.getFormKey())) {
            throw new BusinessException("表单标识创建后不可修改");
        }

        FormDefinition updateEntity = new FormDefinition();
        updateEntity.setId(id);
        // 仅草稿态允许修改元数据
        if (FormStatusEnum.DRAFT.getValue().equals(entity.getStatus())) {
            updateEntity.setFormName(formData.getFormName());
            updateEntity.setDescription(formData.getDescription());
            // 类型亦属元数据：草稿态可改，已发布后固定（防止流程绑定后类型被改走导致流程表单失联）
            updateEntity.setCategory(formData.getCategory());
        }
        updateEntity.setFormJson(formData.getFormJson());
        updateEntity.setOptionsJson(formData.getOptionsJson());
        // 公开开关是访问控制而非身份元数据，任意状态可切换（发布向导/编辑弹窗均可操作）
        updateEntity.setIsPublic(formData.getIsPublic());

        boolean result = this.updateById(updateEntity);
        // 已发布表单规则/公开开关变更直接影响渲染端，需清缓存即时生效
        if (result && FormStatusEnum.PUBLISHED.getValue().equals(entity.getStatus())) {
            this.evictRenderCache(entity.getFormKey());
        }
        return result;
    }

    /**
     * 批量删除表单定义
     *
     * <p>被流程引用的表单禁止删除；其余级联清理关联数据，避免孤儿残留：生成的访问菜单（含其角色授权）、
     * 提交数据、版本快照一并删除</p>
     *
     * @param ids 表单ID列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeFormDefinition(List<Long> ids) {
        List<FormDefinition> forms = this.listByIds(ids);
        this.checkProcessReferences(forms);
        this.removeByIds(ids);
        for (FormDefinition form : forms) {
            // 级联删除已生成的访问菜单（deleteMenu 内部同步清理其角色授权并刷新权限缓存）
            if (form.getMenuId() != null) {
                menuService.deleteMenu(form.getMenuId());
            }
            // 同步清理提交数据与版本快照，防止孤儿数据与死链接
            formDataMapper.delete(new LambdaQueryWrapper<FormData>()
                    .eq(FormData::getFormId, form.getId()));
            formSnapshotMapper.delete(new LambdaQueryWrapper<FormSnapshot>()
                    .eq(FormSnapshot::getFormId, form.getId()));
            // 已发布表单的渲染缓存一并清除，防止删除后仍可渲染
            if (FormStatusEnum.PUBLISHED.getValue().equals(form.getStatus())) {
                this.evictRenderCache(form.getFormKey());
            }
        }
    }

    /**
     * 校验表单是否被流程引用
     *
     * <p>被引用的表单删除后，流程发起与审批时无表单可渲染，故在删除前整体拦截</p>
     *
     * @param forms 待删除表单
     */
    private void checkProcessReferences(List<FormDefinition> forms) {
        // 工作流未启用时无流程服务，跳过校验
        WorkflowModelService workflowModelService = workflowModelServiceProvider.getIfAvailable();
        if (workflowModelService == null || forms.isEmpty()) {
            return;
        }
        Map<String, Set<String>> references = workflowModelService.getFormReferences(
                forms.stream().map(FormDefinition::getFormKey).collect(Collectors.toSet()));
        List<String> blocked = forms.stream()
                .filter(form -> references.containsKey(form.getFormKey()))
                .map(form -> StrUtil.format("表单【{}】被流程【{}】引用", form.getFormName(),
                        String.join("、", references.get(form.getFormKey()))))
                .toList();
        if (!blocked.isEmpty()) {
            throw new BusinessException(String.join("；", blocked) + "，请先解除流程绑定后删除");
        }
    }

    /**
     * 发布表单
     *
     * <p>状态翻转 + 版本自增走单条原子 UPDATE，从 DB 侧消除并发发布的重号竞态；
     * 更新成功后回读最新表单，用同一份规则固化版本快照</p>
     *
     * @param id 表单ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean publishFormDefinition(Long id) {
        FormDefinition entity = this.getById(id);
        if (entity == null) {
            throw new BusinessException("表单不存在");
        }
        boolean result = this.update(new LambdaUpdateWrapper<FormDefinition>()
                .eq(FormDefinition::getId, id)
                .set(FormDefinition::getStatus, FormStatusEnum.PUBLISHED.getValue())
                .setSql("version = version + 1")
        );
        if (result) {
            FormDefinition published = this.getById(id);
            // 固化版本快照：数据回显按提交时版本加载，防止规则变更（字段删除/改名）导致历史数据漂移
            FormSnapshot snapshot = new FormSnapshot();
            snapshot.setFormId(id);
            snapshot.setVersion(published.getVersion());
            snapshot.setFormJson(published.getFormJson());
            snapshot.setOptionsJson(published.getOptionsJson());
            formSnapshotMapper.insert(snapshot);
            this.evictRenderCache(published.getFormKey());
        }
        return result;
    }

    /**
     * 停用表单
     *
     * @param id 表单ID
     */
    @Override
    public boolean disableFormDefinition(Long id) {
        FormDefinition entity = this.getById(id);
        if (entity == null) {
            throw new BusinessException("表单不存在");
        }
        FormDefinition updateEntity = new FormDefinition();
        updateEntity.setId(id);
        updateEntity.setStatus(FormStatusEnum.DISABLED.getValue());
        boolean result = this.updateById(updateEntity);
        if (result) {
            this.evictRenderCache(entity.getFormKey());
        }
        return result;
    }

    /**
     * 按 formKey 获取已发布表单的渲染规则
     *
     * @param formKey 表单唯一标识
     */
    @Override
    public FormRenderVO getFormRender(String formKey) {
        String cacheKey = StrUtil.format(RedisConstants.Form.RENDER_RULE, formKey);
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof FormRenderVO renderVO) {
            return renderVO;
        }
        if (cached != null) {
            // Redis JSON 序列化读回为无类型 Map，转换为 VO
            return BeanUtil.toBean(cached, FormRenderVO.class);
        }

        FormDefinition entity = this.getOne(new LambdaQueryWrapper<FormDefinition>()
                .eq(FormDefinition::getFormKey, formKey)
                .eq(FormDefinition::getStatus, FormStatusEnum.PUBLISHED.getValue())
                .last("LIMIT 1")
        );
        if (entity == null) {
            throw new BusinessException("表单不存在或未发布");
        }
        redisTemplate.opsForValue().set(cacheKey, toRenderVO(entity), RENDER_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        return toRenderVO(entity);
    }

    /**
     * 清除表单渲染缓存
     *
     * @param formKey 表单唯一标识
     */
    private void evictRenderCache(String formKey) {
        redisTemplate.delete(StrUtil.format(RedisConstants.Form.RENDER_RULE, formKey));
    }

    /**
     * 组装表单渲染 VO
     *
     * @param entity 表单定义实体
     */
    private FormRenderVO toRenderVO(FormDefinition entity) {
        FormRenderVO renderVO = new FormRenderVO();
        renderVO.setFormKey(entity.getFormKey());
        renderVO.setFormName(entity.getFormName());
        renderVO.setVersion(entity.getVersion());
        renderVO.setFormJson(entity.getFormJson());
        renderVO.setOptionsJson(entity.getOptionsJson());
        return renderVO;
    }

    /**
     * 按 formKey 获取公开表单的渲染规则（匿名接口）
     *
     * <p>仅返回已发布且 isPublic=1 的表单；直查库，不读 {@link #getFormRender(String)} 的
     * Redis 缓存——缓存 VO 不含 isPublic，命中会绕过公开开关校验</p>
     *
     * @param formKey 表单唯一标识
     */
    @Override
    public FormRenderVO getPublicFormRender(String formKey) {
        FormDefinition entity = this.getOne(new LambdaQueryWrapper<FormDefinition>()
                .eq(FormDefinition::getFormKey, formKey)
                .eq(FormDefinition::getStatus, FormStatusEnum.PUBLISHED.getValue())
                .eq(FormDefinition::getIsPublic, 1)
                .last("LIMIT 1")
        );
        if (entity == null) {
            throw new BusinessException("表单不存在或未开放公开访问");
        }
        return toRenderVO(entity);
    }

    /**
     * 按 formKey 查询表单定义
     *
     * @param formKey 表单唯一标识
     */
    @Override
    public FormDefinition getByFormKey(String formKey) {
        FormDefinition form = this.getOne(new LambdaQueryWrapper<FormDefinition>()
                .eq(FormDefinition::getFormKey, formKey)
                .last("LIMIT 1")
        );
        if (form == null) {
            throw new BusinessException("表单不存在");
        }
        return form;
    }

    /**
     * 生成表单访问菜单并授权可见角色
     *
     * @param formId   表单ID
     * @param menuForm 菜单发布参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveFormMenu(Long formId, FormMenuForm menuForm) {
        FormDefinition form = this.getById(formId);
        if (form == null) {
            throw new BusinessException("表单不存在");
        }
        // 上级菜单必须为目录类型：挂到菜单/按钮下构树异常；根级菜单不套 Layout，得到无侧边栏的孤立页
        // parentId 为空 = 走默认目录"表单中心"（懒创建：首个表单发布时才建目录，避免空目录在侧边栏成点不开的孤菜单）
        Menu parentMenu;
        if (menuForm.getParentId() == null) {
            parentMenu = this.ensureDefaultCatalog();
        } else {
            parentMenu = menuService.getById(menuForm.getParentId());
            if (parentMenu == null) {
                throw new BusinessException("上级菜单不存在");
            }
            if (!MenuTypeEnum.CATALOG.getValue().equals(parentMenu.getType())) {
                throw new BusinessException("上级菜单必须为目录类型");
            }
        }

        // 组装菜单：类型 M，指向通用承载页；已生成过时携带原菜单ID走更新语义（幂等）
        String routeName = "FormRender" + StrUtil.upperFirst(StrUtil.toCamelCase(form.getFormKey(), '_'));
        MenuForm menuFormData = new MenuForm();
        menuFormData.setId(form.getMenuId());
        menuFormData.setParentId(parentMenu.getId());
        menuFormData.setName(menuForm.getMenuName());
        menuFormData.setType(MenuTypeEnum.MENU.getValue());
        menuFormData.setRouteName(routeName);
        menuFormData.setRoutePath(form.getFormKey());
        menuFormData.setComponent("dynamic-form/render");
        menuFormData.setParams(List.of(new KeyValue("formKey", form.getFormKey())));
        menuFormData.setVisible(1);
        menuFormData.setSort(1);
        // keepAlive 不开启：多个表单菜单共用 render 组件，组件级缓存会导致切换表单时回显错乱
        boolean result = menuService.saveMenu(menuFormData);
        if (!result) {
            return false;
        }
        // saveMenu 内部 saveOrUpdate 不回写表单对象主键，按全局唯一路由名称反查菜单ID
        Menu menu = menuService.getOne(new LambdaQueryWrapper<Menu>()
                .eq(Menu::getRouteName, routeName)
                .last("LIMIT 1")
        );

        // 回写 menu_id：下次发布走更新语义，防重复生成菜单
        FormDefinition updateForm = new FormDefinition();
        updateForm.setId(formId);
        updateForm.setMenuId(menu.getId());
        this.updateById(updateForm);

        // 追加式角色授权（沿 tree_path 补齐祖先目录）
        this.assignMenuToRoles(menu.getId(), menuForm.getRoleIds());
        return true;
    }

    /**
     * 确保默认挂载目录"表单中心"存在，不存在则创建
     *
     * <p>挂到顶级"动态表单"目录下，该目录缺失时退化为顶级创建；
     * 无子菜单的目录在侧边栏不渲染</p>
     *
     * @return 默认目录实体
     */
    private Menu ensureDefaultCatalog() {
        // 已有"表单中心"目录（任意层级）直接复用：兼容旧环境顶级目录等历史生成位置
        Menu catalog = menuService.getOne(new LambdaQueryWrapper<Menu>()
                .eq(Menu::getName, DEFAULT_CATALOG_NAME)
                .eq(Menu::getType, MenuTypeEnum.CATALOG.getValue())
                .last("LIMIT 1")
        );
        if (catalog != null) {
            return catalog;
        }
        Menu adminCatalog = menuService.getOne(new LambdaQueryWrapper<Menu>()
                .eq(Menu::getParentId, 0L)
                .eq(Menu::getName, FORM_ADMIN_CATALOG_NAME)
                .eq(Menu::getType, MenuTypeEnum.CATALOG.getValue())
                .last("LIMIT 1")
        );
        // 挂"动态表单"下用相对路由段（父级 /dynamic-form + center）；退化顶级时用绝对路径
        // 退化路径避开 /dynamic-form 前缀：与"动态表单"目录路由并存时会重复注册顶级路由
        boolean underAdminCatalog = adminCatalog != null;

        MenuForm menuFormData = new MenuForm();
        menuFormData.setParentId(underAdminCatalog ? adminCatalog.getId() : 0L);
        menuFormData.setName(DEFAULT_CATALOG_NAME);
        menuFormData.setType(MenuTypeEnum.CATALOG.getValue());
        menuFormData.setRoutePath(underAdminCatalog ? "center" : "/form-center");
        menuFormData.setComponent("Layout");
        menuFormData.setVisible(1);
        menuFormData.setSort(5);
        // EP 图标（前端永久可渲染），避免空/失效图标名导致菜单显示异常
        menuFormData.setIcon("el-icon-EditPen");
        menuService.saveMenu(menuFormData);

        // 目录类型保存时 routeName 被置空，按名称+类型回查
        Menu createdCatalog = menuService.getOne(new LambdaQueryWrapper<Menu>()
                .eq(Menu::getName, DEFAULT_CATALOG_NAME)
                .eq(Menu::getType, MenuTypeEnum.CATALOG.getValue())
                .last("LIMIT 1")
        );
        Assert.notNull(createdCatalog, "默认目录创建失败");
        return createdCatalog;
    }

    /**
     * 获取表单已生成的菜单配置
     *
     * @param formId 表单ID
     */
    @Override
    public FormMenuVO getFormMenu(Long formId) {
        FormDefinition form = this.getById(formId);
        if (form == null || form.getMenuId() == null) {
            return null;
        }
        Menu menu = menuService.getById(form.getMenuId());
        if (menu == null) {
            return null;
        }
        FormMenuVO menuVO = new FormMenuVO();
        menuVO.setMenuId(menu.getId());
        menuVO.setMenuName(menu.getName());
        menuVO.setParentId(menu.getParentId());
        List<RoleMenu> roleMenus = roleMenuService.list(new LambdaQueryWrapper<RoleMenu>()
                .eq(RoleMenu::getMenuId, menu.getId())
        );
        menuVO.setRoleIds(roleMenus.stream().map(RoleMenu::getRoleId).distinct().toList());
        return menuVO;
    }

    /**
     * 追加式角色授权：向角色授予菜单及 tree_path 祖先目录
     *
     * <p>踩坑1：只授子菜单不授父目录，buildRoutes 构树出孤儿节点，侧边栏不渲染；
     * 踩坑2：不能复用 assignMenusToRole——它是整体替换语义，会清空角色原有菜单授权</p>
     *
     * @param menuId  菜单ID
     * @param roleIds 角色ID列表
     */
    private void assignMenuToRoles(Long menuId, List<Long> roleIds) {
        if (CollectionUtil.isEmpty(roleIds)) {
            return;
        }
        // 授权集合 = 菜单本身 + tree_path 祖先目录（如 "0,11" → 补 11，根节点 0 排除）
        Menu menu = menuService.getById(menuId);
        Set<Long> menuIdsToGrant = new HashSet<>();
        menuIdsToGrant.add(menuId);
        if (StrUtil.isNotBlank(menu.getTreePath())) {
            Arrays.stream(menu.getTreePath().split(","))
                    .map(String::trim)
                    .filter(StrUtil::isNotBlank)
                    .filter(id -> !"0".equals(id))
                    .forEach(id -> menuIdsToGrant.add(Long.parseLong(id)));
        }

        // 查询已有关联，仅补插缺失项
        List<RoleMenu> existing = roleMenuService.list(new LambdaQueryWrapper<RoleMenu>()
                .in(RoleMenu::getRoleId, roleIds)
                .in(RoleMenu::getMenuId, menuIdsToGrant)
        );
        Set<String> existingKeys = existing.stream()
                .map(rm -> rm.getRoleId() + ":" + rm.getMenuId())
                .collect(Collectors.toSet());

        List<RoleMenu> toInsert = roleIds.stream()
                .flatMap(roleId -> menuIdsToGrant.stream()
                        .filter(grantMenuId -> !existingKeys.contains(roleId + ":" + grantMenuId))
                        .map(grantMenuId -> new RoleMenu(roleId, grantMenuId))
                )
                .toList();
        if (CollectionUtil.isNotEmpty(toInsert)) {
            roleMenuService.saveBatch(toInsert);
            // 授权变化后刷新角色权限缓存
            roleMenuService.refreshRolePermsCache();
        }
    }

    /**
     * 审批表单下拉选项（供工作流设计器绑定表单）
     *
     * <p>仅返回已发布且类型为 workflow 的表单；value 为 formKey（BPMN formKey 引用），
     * 草稿/停用表单结构未定不应被流程绑定</p>
     *
     * @return 审批表单下拉选项列表
     */
    @Override
    public List<Option<String>> listWorkflowFormOptions() {
        return this.list(new LambdaQueryWrapper<FormDefinition>()
                        .eq(FormDefinition::getCategory, FormCategoryEnum.WORKFLOW.getValue())
                        .eq(FormDefinition::getStatus, FormStatusEnum.PUBLISHED.getValue())
                        .select(FormDefinition::getFormKey, FormDefinition::getFormName)
                        .orderByAsc(FormDefinition::getCreateTime)
                ).stream()
                .map(form -> new Option<>(form.getFormKey(), form.getFormName()))
                .toList();
    }

    //---------------------------------------------------
    // AI 辅助：未开启 AI 时调用返回业务异常
    //---------------------------------------------------

    /**
     * 用 AI 把需求描述转成 form-create 规则
     *
     * @param description 需求描述
     * @return 通过结构校验的 form-create rule JSON 字符串
     */
    @Override
    public String aiGenerateRule(String description) {
        List<Map<String, Object>> rules = aiChatService.chatJsonList(
                promptLoader.get(SYSTEM_PROMPT_PATH), buildAiUserPrompt(description));
        return FormRuleValidator.validateRule(JSONUtil.toJsonStr(rules));
    }

    /**
     * 构造用户提示词
     *
     * @param description 需求描述
     * @return 用户提示词
     */
    private String buildAiUserPrompt(String description) {
        return "需求描述：\n" + description;
    }

}
