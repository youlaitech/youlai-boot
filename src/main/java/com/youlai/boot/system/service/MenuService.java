package com.youlai.boot.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.youlai.boot.system.model.dto.MenuCreateDTO;
import com.youlai.boot.system.model.form.MenuForm;
import com.youlai.boot.common.model.Option;
import com.youlai.boot.system.model.entity.Menu;
import com.youlai.boot.system.model.query.MenuQuery;
import com.youlai.boot.system.model.vo.MenuVO;
import com.youlai.boot.system.model.vo.RouteVO;

import java.util.List;

/**
 * 菜单业务接口
 *
 * @author haoxr
 * @since 2020/11/06
 */
public interface MenuService extends IService<Menu> {

    /**
     * 获取菜单表格列表
     */
    List<MenuVO> listMenus(MenuQuery queryParams);

    /**
     * 获取菜单下拉列表
     *
     * @param onlyParent 是否只查询父级菜单
     */
    List<Option<Long>> listMenuOptions(boolean onlyParent);

    /**
     * 新增菜单
     *
     * @param menuForm  菜单表单对象
     */
    boolean saveMenu(MenuForm menuForm);

    /**
     * 获取当前用户的菜单路由列表
     */
    List<RouteVO> listCurrentUserRoutes();

    /**
     * 获取当前用户的菜单路由列表（指定数据源）
     *
     * @param datasource 数据源名称，如：master(主库)、naiveui(NaiveUI数据库)、template(模板数据库)
     */
    List<RouteVO> listCurrentUserRoutes(String datasource);

    /**
     * 修改菜单显示状态
     *
     * @param menuId 菜单ID
     * @param visible 是否显示(1-显示 0-隐藏)
     */
    boolean updateMenuVisible(Long menuId, Integer visible);

    /**
     * 获取菜单表单数据
     *
     * @param id 菜单ID
     */
    MenuForm getMenuForm(Long id);

    /**
     * 删除菜单
     *
     * @param id 菜单ID
     */
    boolean deleteMenu(Long id);

    /**
     * 为代码生成创建菜单
     *
     * <p>菜单已存在（routeName 相同）时直接返回；创建成功后一并生成查询/新增/修改/删除四个按钮权限。
     *
     * @param parentMenuId 父菜单ID
     * @param dto          创建菜单数据载体
     */
    void addMenuForCodegen(Long parentMenuId, MenuCreateDTO dto);
}
