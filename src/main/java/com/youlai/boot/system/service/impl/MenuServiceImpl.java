package com.youlai.boot.system.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.boot.common.base.IBaseEnum;
import com.youlai.boot.common.exception.BusinessException;
import com.youlai.boot.framework.security.util.SecurityUtils;
import com.youlai.boot.support.ai.AiChatService;
import com.youlai.boot.support.ai.PromptLoader;
import com.youlai.boot.system.converter.MenuConverter;
import com.youlai.boot.system.mapper.MenuMapper;
import com.youlai.boot.system.model.dto.MenuCreateDTO;
import com.youlai.boot.system.model.entity.Menu;
import com.youlai.boot.system.model.form.MenuAiFillForm;
import com.youlai.boot.system.model.form.MenuForm;
import com.youlai.boot.system.model.query.MenuQuery;
import com.youlai.boot.system.model.vo.MenuAiVO;
import com.youlai.boot.system.model.vo.MenuVO;
import com.youlai.boot.system.model.vo.RouteVO;
import com.youlai.boot.common.constant.SystemConstants;
import com.youlai.boot.system.enums.MenuTypeEnum;
import com.youlai.boot.common.enums.StatusEnum;
import com.youlai.boot.common.model.KeyValue;
import com.youlai.boot.common.model.Option;
import com.youlai.boot.system.service.MenuService;
import com.youlai.boot.system.service.RoleMenuService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 菜单服务实现类
 *
 * @author Ray.Hao
 * @since 2020/11/06
 */
@Service
@RequiredArgsConstructor
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu> implements MenuService {

    private final MenuConverter menuConverter;

    private final RoleMenuService roleMenuService;

    /** 系统提示词名称，对应 resources/prompts 下的相对路径 */
    private static final String SYSTEM_PROMPT_PATH = "menu/system.md";

    private final AiChatService aiChatService;
    private final PromptLoader promptLoader;


    /**
     * 菜单列表
     *
     * @param queryParams {@link MenuQuery}
     */
    @Override
    public List<MenuVO> listMenus(MenuQuery queryParams) {
        List<Menu> menus = this.list(new LambdaQueryWrapper<Menu>()
                .like(StrUtil.isNotBlank(queryParams.getKeywords()), Menu::getName, queryParams.getKeywords())
                .orderByAsc(Menu::getSort)
        );
        // 获取所有菜单ID
        Set<Long> menuIds = menus.stream()
                .map(Menu::getId)
                .collect(Collectors.toSet());

        // 获取所有父级ID
        Set<Long> parentIds = menus.stream()
                .map(Menu::getParentId)
                .collect(Collectors.toSet());

        // 获取根节点ID（递归的起点），即父节点ID中不包含在部门ID中的节点，注意这里不能拿顶级菜单 O 作为根节点，因为菜单筛选的时候 O 会被过滤掉
        List<Long> rootIds = parentIds.stream()
                .filter(id -> !menuIds.contains(id))
                .toList();

        // 使用递归函数来构建菜单树
        return rootIds.stream()
                .flatMap(rootId -> buildMenuTree(rootId, menus).stream())
                .collect(Collectors.toList());
    }

    /**
     * 递归生成菜单列表
     *
     * @param parentId 父级ID
     * @param menuList 菜单列表
     * @return 菜单列表
     */
    private List<MenuVO> buildMenuTree(Long parentId, List<Menu> menuList) {
        return CollectionUtil.emptyIfNull(menuList)
                .stream()
                .filter(menu -> menu.getParentId().equals(parentId))
                .map(entity -> {
                    MenuVO menuVo = menuConverter.toVo(entity);
                    List<MenuVO> children = buildMenuTree(entity.getId(), menuList);
                    menuVo.setChildren(children);
                    return menuVo;
                }).toList();
    }

    /**
     * 菜单下拉数据
     *
     * @param types 菜单类型，为空查全部
     */
    @Override
    public List<Option<Long>> listMenuOptions(List<String> types) {
        List<Menu> menuList = this.list(new LambdaQueryWrapper<Menu>()
                .in(CollectionUtil.isNotEmpty(types), Menu::getType, types)
                .orderByAsc(Menu::getSort)
        );
        return buildMenuOptions(SystemConstants.ROOT_NODE_ID, menuList);
    }

    /**
     * 递归生成菜单下拉层级列表
     *
     * @param parentId 父级ID
     * @param menuList 菜单列表
     * @return 菜单下拉列表
     */
    private List<Option<Long>> buildMenuOptions(Long parentId, List<Menu> menuList) {
        List<Option<Long>> menuOptions = new ArrayList<>();

        for (Menu menu : menuList) {
            if (menu.getParentId().equals(parentId)) {
                Option<Long> option = new Option<>(menu.getId(), menu.getName());
                List<Option<Long>> subMenuOptions = buildMenuOptions(menu.getId(), menuList);
                if (!subMenuOptions.isEmpty()) {
                    option.setChildren(subMenuOptions);
                }
                menuOptions.add(option);
            }
        }

        return menuOptions;
    }

    /**
     * 获取当前用户的菜单路由列表
     */
    @Override
    public List<RouteVO> listCurrentUserRoutes() {
        Set<String> roleCodes = SecurityUtils.getRoles();

        if (CollectionUtil.isEmpty(roleCodes)) {
            return Collections.emptyList();
        }

        List<Menu> menuList;
        if (SecurityUtils.isRoot()) {
            // 超级管理员获取所有菜单
            menuList = this.list(new LambdaQueryWrapper<Menu>()
                    .ne(Menu::getType, MenuTypeEnum.BUTTON.getValue())
                    .orderByAsc(Menu::getSort)
            );
        } else {
            // 普通用户：通过角色获取菜单（权限控制已过滤）
            menuList = this.baseMapper.getMenusByRoleCodes(roleCodes);

            // 双重保障：动态查询"平台管理"目录，过滤其子菜单
            // 通过路由路径识别平台管理目录，避免硬编码
            Menu platformMenu = this.getOne(new LambdaQueryWrapper<Menu>()
                    .eq(Menu::getRoutePath, "/support")
                    .eq(Menu::getParentId, SystemConstants.ROOT_NODE_ID)
                    .eq(Menu::getType, MenuTypeEnum.CATALOG.getValue())
                    .last("LIMIT 1")
            );

            if (platformMenu != null) {
                final Long platformMenuId = platformMenu.getId();
                menuList = menuList.stream()
                        .filter(menu -> {
                            String treePath = menu.getTreePath();
                            // 排除平台管理目录及其子菜单
                            // treePath 格式：0,1 或 0,1,110 等
                            return treePath == null ||
                                    (!treePath.startsWith("0," + platformMenuId + ",") &&
                                     !treePath.equals("0," + platformMenuId));
                        })
                        .collect(Collectors.toList());
            }
        }
        return buildRoutes(SystemConstants.ROOT_NODE_ID, menuList);
    }

    /**
     * 获取当前用户的菜单路由列表（指定数据源）
     *
     * @param datasource 数据源名称
     *                   - master: 主库菜单数据
     *                   - naiveui: NaiveUI项目菜单数据
     *                   - template: 模板项目菜单数据
     */
    @Override
    public List<RouteVO> listCurrentUserRoutes(String datasource) {
        return listCurrentUserRoutes();
    }


    /**
     * 递归生成菜单路由层级列表
     *
     * @param parentId 父级ID
     * @param menuList 菜单列表
     * @return 路由层级列表
     */
    private List<RouteVO> buildRoutes(Long parentId, List<Menu> menuList) {
        List<RouteVO> routeList = new ArrayList<>();

        for (Menu menu : menuList) {
            if (menu.getParentId().equals(parentId)) {
                RouteVO routeVo = toRouteVo(menu);
                List<RouteVO> children = buildRoutes(menu.getId(), menuList);
                if (!children.isEmpty()) {
                    routeVo.setChildren(children);
                }
                routeList.add(routeVo);
            }
        }

        return routeList;
    }

    /**
     * 根据RouteBO创建RouteVo
     */
    private RouteVO toRouteVo(Menu menu) {
        RouteVO routeVo = new RouteVO();
        String routePath = menu.getRoutePath();
        String externalUrl = menu.getExternalUrl();

        // 外链(type=E)：component 为空是新标签页，component=iframe 是系统内嵌
        boolean isExternal = MenuTypeEnum.EXTERNAL.getValue().equals(menu.getType());
        boolean isEmbedded = isExternal && "iframe".equals(menu.getComponent());

        // 新标签页：path 用外链地址；内嵌：path 用内部路由路径
        String path = isExternal && !isEmbedded && StrUtil.isNotBlank(externalUrl)
                ? externalUrl
                : routePath;
        routeVo.setPath(path);

        // 路由名称：外链不设 name（前端 filterRoutes 会过滤出路由注册表）
        String routeName = menu.getRouteName();
        if (StrUtil.isBlank(routeName) && !isExternal) {
            routeName = StringUtils.capitalize(StrUtil.toCamelCase(path, '-'));
        }
        routeVo.setName(routeName);

        routeVo.setRedirect(menu.getRedirect());

        if (isEmbedded) {
            routeVo.setComponent("iframe");
        } else if (isExternal) {
            routeVo.setComponent(null);
        } else {
            routeVo.setComponent(menu.getComponent());
        }

        RouteVO.Meta meta = new RouteVO.Meta();
        meta.setTitle(menu.getName());
        meta.setIcon(menu.getIcon());
        meta.setHidden(StatusEnum.DISABLE.getValue().equals(menu.getVisible()));
        // 【菜单/内嵌外链】是否开启页面缓存
        if ((MenuTypeEnum.MENU.getValue().equals(menu.getType()) || isEmbedded)
                && ObjectUtil.equals(menu.getKeepAlive(), 1)) {
            meta.setKeepAlive(true);
        }
        if (isEmbedded && StrUtil.isNotBlank(externalUrl)) {
            meta.setExternalUrl(externalUrl);
        }

        Map<String, Object> paramsMap = menu.getParams();
        if (paramsMap != null && !paramsMap.isEmpty()) {
            Map<String, String> paramMap = paramsMap.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> String.valueOf(e.getValue())));
            meta.setParams(paramMap);
        }
        routeVo.setMeta(meta);
        return routeVo;
    }

    /**
     * 新增/修改菜单
     */
    @Override
    @CacheEvict(cacheNames = "menu", key = "'routes'")
    public boolean saveMenu(MenuForm menuForm) {

        String menuType = menuForm.getType();
        boolean isExternal = MenuTypeEnum.EXTERNAL.getValue().equals(menuType);
        boolean isEmbedded = isExternal && "iframe".equals(menuForm.getComponent());

        if (MenuTypeEnum.CATALOG.getValue().equals(menuType)) {  // 如果是目录
            String path = menuForm.getRoutePath();
            if (menuForm.getParentId() == 0 && !path.startsWith("/")) {
                menuForm.setRoutePath("/" + path); // 一级目录需以 / 开头
            }
            menuForm.setComponent("Layout");
        } else if (isExternal && !isEmbedded) {
            // 外链新标签页：component 留空
            menuForm.setComponent(null);
        }
        if (Objects.equals(menuForm.getParentId(), menuForm.getId())) {
            throw new BusinessException("父级菜单不能为当前菜单");
        }
        validateMenuParent(menuForm);
        Menu entity = menuConverter.toEntity(menuForm);
        String treePath = generateMenuTreePath(menuForm.getParentId());
        entity.setTreePath(treePath);

        // 新增菜单未指定排序时排到同级末尾
        if (menuForm.getId() == null && entity.getSort() == null) {
            entity.setSort(resolveNextSort(menuForm.getParentId()));
        }

        List<KeyValue> params = menuForm.getParams();
        if (CollectionUtil.isNotEmpty(params)) {
            entity.setParams(params.stream()
                    .collect(Collectors.toMap(KeyValue::getKey, KeyValue::getValue)));
        } else {
            entity.setParams(null);
        }
        // 菜单(M)和内嵌外链(E+iframe)需要路由名称唯一
        boolean needsRouteName = MenuTypeEnum.MENU.getValue().equals(menuType) || isEmbedded;
        if (needsRouteName) {
            Assert.isFalse(this.exists(new LambdaQueryWrapper<Menu>()
                    .eq(Menu::getRouteName, entity.getRouteName())
                    .ne(menuForm.getId() != null, Menu::getId, menuForm.getId())
            ), "路由名称已存在");
        } else {
            // 其他类型时 给路由名称赋值为空
            entity.setRouteName(null);
        }

        boolean result = this.saveOrUpdate(entity);
        if (result) {
            if (menuForm.getId() != null) {
                // 编辑刷新角色权限缓存
                roleMenuService.refreshRolePermsCache();
            } else if (Boolean.TRUE.equals(menuForm.getGenerateCrudButtons())
                    && StrUtil.isNotBlank(menuForm.getButtonPermPrefix())) {
                // 新增页面菜单时按需生成增删改查按钮
                saveCrudButtons(entity.getId(), menuForm.getButtonPermPrefix().trim(), treePath);
            }
        }
        // 修改菜单如果有子菜单，则更新子菜单的树路径
        updateChildrenTreePath(entity.getId(), treePath);
        return result;
    }

    /**
     * 更新子菜单树路径
     *
     * @param id       当前菜单ID
     * @param treePath 当前菜单树路径
     */
    private void updateChildrenTreePath(Long id, String treePath) {
        List<Menu> children = this.list(new LambdaQueryWrapper<Menu>().eq(Menu::getParentId, id));
        if (CollectionUtil.isNotEmpty(children)) {
            // 子菜单的树路径等于父菜单的树路径加上父菜单ID
            String childTreePath = treePath + "," + id;
            this.update(new LambdaUpdateWrapper<Menu>()
                    .eq(Menu::getParentId, id)
                    .set(Menu::getTreePath, childTreePath)
            );
            for (Menu child : children) {
                // 递归更新子菜单
                updateChildrenTreePath(child.getId(), childTreePath);
            }
        }
    }

    /**
     * 部门路径生成
     *
     * @param parentId 父ID
     * @return 父节点路径以英文逗号(, )分割，eg: 1,2,3
     */
    private String generateMenuTreePath(Long parentId) {
        if (SystemConstants.ROOT_NODE_ID.equals(parentId)) {
            return String.valueOf(parentId);
        } else {
            Menu parent = this.getById(parentId);
            return parent != null ? parent.getTreePath() + "," + parent.getId() : null;
        }
    }

    /**
     * 同级菜单的最大排序 + 1
     *
     * @param parentId 父菜单ID
     * @return 下一个排序值
     */
    private int resolveNextSort(Long parentId) {
        Menu maxSortMenu = this.getOne(new LambdaQueryWrapper<Menu>()
                .eq(Menu::getParentId, parentId)
                .orderByDesc(Menu::getSort)
                .last("LIMIT 1")
        );
        return maxSortMenu == null || maxSortMenu.getSort() == null ? 1 : maxSortMenu.getSort() + 1;
    }

    /**
     * 校验上级菜单层级：按钮只能挂在菜单下，其他类型只能挂在顶级或目录下
     *
     * @param menuForm 菜单表单
     */
    private void validateMenuParent(MenuForm menuForm) {
        // 父级没变时跳过校验，让历史遗留的非法层级仍可编辑其他字段
        if (menuForm.getId() != null) {
            Menu current = this.getById(menuForm.getId());
            if (current != null && Objects.equals(current.getParentId(), menuForm.getParentId())) {
                return;
            }
        }

        boolean isButton = MenuTypeEnum.BUTTON.getValue().equals(menuForm.getType());
        Long parentId = menuForm.getParentId();
        if (parentId == null || SystemConstants.ROOT_NODE_ID.equals(parentId)) {
            Assert.isFalse(isButton, "按钮权限只能挂在菜单下");
            return;
        }

        Menu parent = this.getById(parentId);
        Assert.notNull(parent, "上级菜单不存在");
        if (isButton) {
            Assert.isTrue(MenuTypeEnum.MENU.getValue().equals(parent.getType()), "按钮权限只能挂在菜单下");
        } else {
            Assert.isFalse(MenuTypeEnum.MENU.getValue().equals(parent.getType()), "菜单下只能挂按钮权限");
        }
    }

    /**
     * 生成增删改查按钮权限
     *
     * @param menuId       所属菜单ID
     * @param permPrefix   权限标识前缀，不含末尾冒号，如 sys:user
     * @param menuTreePath 所属菜单的树路径
     */
    private void saveCrudButtons(Long menuId, String permPrefix, String menuTreePath) {
        String[] names = {"查询", "新增", "修改", "删除"};
        String[] actions = {"list", "create", "update", "delete"};
        // 树路径记录祖先链，等于所属菜单的树路径加上所属菜单ID，删除菜单时据此级联
        String buttonTreePath = menuTreePath + "," + menuId;

        for (int i = 0; i < names.length; i++) {
            Menu button = new Menu();
            button.setParentId(menuId);
            button.setType(MenuTypeEnum.BUTTON.getValue());
            button.setName(names[i]);
            button.setPerm(permPrefix + ":" + actions[i]);
            button.setSort(i + 1);
            button.setVisible(1);
            button.setTreePath(buttonTreePath);
            this.save(button);
        }
    }


    /**
     * 修改菜单显示状态
     *
     * @param menuId  菜单ID
     * @param visible 是否显示(1->显示；2->隐藏)
     * @return 是否修改成功
     */
    @Override
    @CacheEvict(cacheNames = "menu", key = "'routes'")
    public boolean updateMenuVisible(Long menuId, Integer visible) {
        return this.update(new LambdaUpdateWrapper<Menu>()
                .eq(Menu::getId, menuId)
                .set(Menu::getVisible, visible)
        );
    }

    /**
     * 获取菜单表单数据
     *
     * @param id 菜单ID
     * @return 菜单表单数据
     */
    @Override
    public MenuForm getMenuForm(Long id) {
        Menu entity = this.getById(id);
        Assert.isTrue(entity != null, "菜单不存在");
        MenuForm formData = menuConverter.toForm(entity);
        // 路由参数 Map 转换为 [{key:"id", value:"1"}, {key:"name", value:"张三"}]
        Map<String, Object> paramsMap = entity.getParams();
        if (paramsMap != null && !paramsMap.isEmpty()) {
            List<KeyValue> transformedList = paramsMap.entrySet().stream()
                    .map(entry -> new KeyValue(entry.getKey(), String.valueOf(entry.getValue())))
                    .toList();
            formData.setParams(transformedList);
        }
        return formData;
    }

    /**
     * 删除菜单
     *
     * @param id 菜单ID
     * @return 是否删除成功
     */
    @Override
    @CacheEvict(cacheNames = "menu", key = "'routes'")
    public boolean deleteMenu(Long id) {
        boolean result = this.remove(new LambdaQueryWrapper<Menu>()
                .eq(Menu::getId, id)
                .or()
                .apply("CONCAT (',',tree_path,',') LIKE CONCAT('%,',{0},',%')", id));


        // 刷新角色权限缓存
        if (result) {
            roleMenuService.refreshRolePermsCache();
        }
        return result;

    }

    /**
     * 为代码生成创建菜单
     *
     * @param parentMenuId 父菜单ID
     * @param dto          创建菜单数据载体
     */
    @Override
    public void addMenuForCodegen(Long parentMenuId, MenuCreateDTO dto) {
        Menu parentMenu = this.getById(parentMenuId);
        Assert.notNull(parentMenu, "上级菜单不存在");

        long count = this.count(new LambdaQueryWrapper<Menu>().eq(Menu::getRouteName, dto.routeName()));
        if (count > 0) {
            return;
        }

        int sort = resolveNextSort(parentMenuId);

        Menu menu = new Menu();
        menu.setParentId(parentMenuId);
        menu.setName(dto.name());

        menu.setRouteName(dto.routeName());
        menu.setRoutePath(StrUtil.toSymbolCase(dto.routeName(), '-'));
        menu.setComponent(dto.moduleName() + "/" + StrUtil.toSymbolCase(dto.routeName(), '-') + "/index");
        menu.setType(MenuTypeEnum.MENU.getValue());
        menu.setSort(sort);
        menu.setVisible(1);
        boolean result = this.save(menu);

        if (result) {
            // 生成treePath
            String treePath = generateMenuTreePath(parentMenuId);
            menu.setTreePath(treePath);
            this.updateById(menu);

            // 生成 CRUD 按钮权限
            saveCrudButtons(menu.getId(), dto.moduleName() + ":" + dto.tableName().replace("_", "-"), treePath);
        }
    }

    //---------------------------------------------------
    // AI 辅助：未开启 AI 时调用返回业务异常
    //---------------------------------------------------

    /**
     * 用 AI 推断菜单的访问路径、权限标识与配套按钮
     *
     * @param form 菜单信息
     * @return 推断结果
     */
    @Override
    public MenuAiVO aiFillMenu(MenuAiFillForm form) {
        return aiChatService.chatEntity(
                promptLoader.get(SYSTEM_PROMPT_PATH), buildAiUserPrompt(form), MenuAiVO.class);
    }

    /**
     * 构造用户提示词，附带同级菜单样本供模型沿用既有命名风格
     *
     * @param form 菜单信息
     * @return 用户提示词
     */
    private String buildAiUserPrompt(MenuAiFillForm form) {
        StringBuilder text = new StringBuilder();
        text.append("菜单名称：").append(form.getName()).append("\n");
        text.append("菜单类型：").append(menuTypeLabel(form.getType())).append("\n");

        Long parentId = form.getParentId() == null ? SystemConstants.ROOT_NODE_ID : form.getParentId();
        Menu parent = this.getById(parentId);
        text.append("上级菜单：").append(parent == null ? "顶级" : parent.getName()).append("\n");

        List<Menu> siblings = this.list(new LambdaQueryWrapper<Menu>()
                .eq(Menu::getParentId, parentId)
                .orderByAsc(Menu::getSort)
                .last("LIMIT 10")
        );
        text.append("同级菜单示例：\n");
        if (CollectionUtil.isEmpty(siblings)) {
            text.append("（无）\n");
        } else {
            siblings.forEach(menu -> text.append("- ")
                    .append(menu.getName())
                    .append("（类型 ").append(menuTypeLabel(menu.getType()))
                    .append("，路径片段 ").append(StrUtil.blankToDefault(menu.getRoutePath(), "无"))
                    .append("，权限 ").append(StrUtil.blankToDefault(menu.getPerm(), "无"))
                    .append("）\n"));
        }
        return text.toString();
    }

    /**
     * 菜单类型取中文名称，枚举无匹配时沿用原值
     *
     * @param type 菜单类型
     * @return 中文名称
     */
    private String menuTypeLabel(String type) {
        if (StrUtil.isBlank(type)) {
            return "未指定";
        }
        MenuTypeEnum menuType = IBaseEnum.getEnumByValue(type, MenuTypeEnum.class);
        return menuType == null ? type : menuType.getLabel();
    }

}
