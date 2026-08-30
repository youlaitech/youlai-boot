package com.youlai.boot.module.form.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.common.exception.BusinessException;
import com.youlai.boot.common.model.KeyValue;
import com.youlai.boot.module.form.converter.FormConverter;
import com.youlai.boot.module.form.enums.FormStatusEnum;
import com.youlai.boot.module.form.mapper.FormDefinitionMapper;
import com.youlai.boot.module.form.mapper.FormSnapshotMapper;
import com.youlai.boot.module.form.model.entity.FormDefinition;
import com.youlai.boot.module.form.model.entity.FormSnapshot;
import com.youlai.boot.module.form.model.form.FormDefinitionForm;
import com.youlai.boot.module.form.model.form.FormMenuForm;
import com.youlai.boot.module.form.model.query.FormDefinitionQuery;
import com.youlai.boot.module.form.model.vo.FormDefinitionPageVO;
import com.youlai.boot.module.form.model.vo.FormMenuVO;
import com.youlai.boot.module.form.model.vo.FormRenderVO;
import com.youlai.boot.module.form.service.FormDefinitionService;
import com.youlai.boot.system.enums.MenuTypeEnum;
import com.youlai.boot.system.model.entity.Menu;
import com.youlai.boot.system.model.entity.RoleMenu;
import com.youlai.boot.system.model.form.MenuForm;
import com.youlai.boot.system.service.MenuService;
import com.youlai.boot.system.service.RoleMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 表单定义业务实现类
 *
 * @author Ray.Hao
 * @since 4.8.0
 */
@Service
@RequiredArgsConstructor
public class FormDefinitionServiceImpl extends ServiceImpl<FormDefinitionMapper, FormDefinition> implements FormDefinitionService {

    private final FormConverter formConverter;
    private final RedisTemplate<String, Object> redisTemplate;
    private final MenuService menuService;
    private final RoleMenuService roleMenuService;
    private final FormSnapshotMapper formSnapshotMapper;

    /**
     * 表单定义分页列表
     *
     * @param queryParams 查询参数
     */
    @Override
    public Page<FormDefinitionPageVO> getFormDefinitionPage(FormDefinitionQuery queryParams) {
        int pageNum = queryParams.getPageNum();
        int pageSize = queryParams.getPageSize();
        return this.baseMapper.getFormDefinitionPage(new Page<>(pageNum, pageSize), queryParams);
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
        if (entity.getFormJson() == null) {
            entity.setFormJson("[]");
        }
        return this.save(entity);
    }

    /**
     * 修改表单定义
     *
     * <p>formKey 创建后不可修改；草稿态可修改元数据与规则，
     * 非草稿态仅可修改规则（元数据只读，防止已发布表单的语义漂移）</p>
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
     * @param ids 表单ID列表
     */
    @Override
    public void removeFormDefinition(List<Long> ids) {
        List<FormDefinition> forms = this.listByIds(ids);
        this.removeByIds(ids);
        // 已发布表单的渲染缓存一并清除，防止删除后仍可渲染
        forms.stream()
                .filter(form -> FormStatusEnum.PUBLISHED.getValue().equals(form.getStatus()))
                .forEach(form -> this.evictRenderCache(form.getFormKey()));
    }

    /**
     * 发布表单
     *
     * @param id 表单ID
     */
    @Override
    public boolean publishFormDefinition(Long id) {
        FormDefinition entity = this.getById(id);
        if (entity == null) {
            throw new BusinessException("表单不存在");
        }
        FormDefinition updateEntity = new FormDefinition();
        updateEntity.setId(id);
        updateEntity.setStatus(FormStatusEnum.PUBLISHED.getValue());
        // 每次发布版本+1，供数据提交时记录版本快照
        int newVersion = entity.getVersion() + 1;
        updateEntity.setVersion(newVersion);
        boolean result = this.updateById(updateEntity);
        if (result) {
            // 固化版本快照：数据回显按提交时版本加载，防止规则变更（字段删除/改名）导致历史数据漂移
            FormSnapshot snapshot = new FormSnapshot();
            snapshot.setFormId(id);
            snapshot.setVersion(newVersion);
            snapshot.setFormJson(entity.getFormJson());
            snapshot.setOptionsJson(entity.getOptionsJson());
            formSnapshotMapper.insert(snapshot);
            this.evictRenderCache(entity.getFormKey());
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
        String cacheKey = RedisConstants.Form.RENDER_RULE.replace("{}", formKey);
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

        FormRenderVO renderVO = new FormRenderVO();
        renderVO.setFormKey(entity.getFormKey());
        renderVO.setFormName(entity.getFormName());
        renderVO.setVersion(entity.getVersion());
        renderVO.setFormJson(entity.getFormJson());
        renderVO.setOptionsJson(entity.getOptionsJson());
        redisTemplate.opsForValue().set(cacheKey, renderVO);
        return renderVO;
    }

    /**
     * 清除表单渲染缓存
     *
     * @param formKey 表单唯一标识
     */
    private void evictRenderCache(String formKey) {
        redisTemplate.delete(RedisConstants.Form.RENDER_RULE.replace("{}", formKey));
    }

    /**
     * 按 formKey 获取公开表单的渲染规则（匿名接口）
     *
     * <p>不复用 {@link #getFormRender(String)} 的 Redis 缓存：缓存 VO 不含 isPublic，
     * 开关切换需额外失效逻辑；公开页非高频路径（IP 全局限流兜底），直查更简单可靠</p>
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

        FormRenderVO renderVO = new FormRenderVO();
        renderVO.setFormKey(entity.getFormKey());
        renderVO.setFormName(entity.getFormName());
        renderVO.setVersion(entity.getVersion());
        renderVO.setFormJson(entity.getFormJson());
        renderVO.setOptionsJson(entity.getOptionsJson());
        return renderVO;
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

    /** 默认挂载目录名称：发布向导未显式选目录时自动使用/创建 */
    private static final String DEFAULT_CATALOG_NAME = "表单中心";

    /**
     * 生成表单访问菜单并授权可见角色
     *
     * <p>菜单 component 指向通用承载页 dynamic-form/render，路由参数携带 formKey，
     * 承载页从 route.meta.params 读取后按 formKey 加载渲染规则</p>
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

    /** 表单管理目录名称（顶级，form.sql 预置）："表单中心"默认挂载其下，顶级入口聚合 */
    private static final String FORM_ADMIN_CATALOG_NAME = "动态表单";

    /**
     * 确保默认挂载目录"表单中心"存在，不存在则创建
     *
     * <p>挂载位置："动态表单"目录下（顶级入口聚合，员工侧边栏为 动态表单 > 表单中心 > 各表单；
     * 管理菜单不授权给填写角色即不渲染，无需顶级分离）；
     * "动态表单"目录缺失时（未执行 form.sql 的环境）退化为顶级创建兜底</p>
     *
     * <p>懒创建而非 SQL 预置：预置空目录在侧边栏是点不开的孤菜单（无子路由不注册）；
     * 首个表单发布时才建目录，目录一出现就有子菜单</p>
     *
     * <p>踩坑：saveMenu 内部 saveOrUpdate 不回写表单对象主键，需按路由名称反查实体</p>
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
        menuFormData.setRouteName("FormCenter");
        menuFormData.setRoutePath(underAdminCatalog ? "center" : "/form-center");
        menuFormData.setComponent("Layout");
        // alwaysShow=1：角色仅有单个表单入口时父级目录不被侧边栏折叠吞掉
        menuFormData.setAlwaysShow(1);
        menuFormData.setVisible(1);
        menuFormData.setSort(5);
        // EP 图标（前端永久可渲染），避免空/失效图标名导致菜单显示异常
        menuFormData.setIcon("el-icon-EditPen");
        menuService.saveMenu(menuFormData);

        return menuService.getOne(new LambdaQueryWrapper<Menu>()
                .eq(Menu::getRouteName, "FormCenter")
                .last("LIMIT 1")
        );
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

}
