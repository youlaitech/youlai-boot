package com.youlai.boot.system.model.dto;

/**
 * 创建菜单DTO
 *
 * <p>承载创建菜单及其 CRUD 按钮权限所需的全部信息，由调用方（如代码生成）构造后
 * 传给 {@link com.youlai.boot.system.service.MenuService#addMenuForCodegen(Long, MenuCreateDTO)}，
 * 使 system 模块无需依赖调用方的实体类型。</p>
 *
 * @author Ray.Hao
 * @since 4.8.0
 */
public record MenuCreateDTO(

        /** 菜单名称 */
        String name,

        /** 路由名称，同时作为前端组件目录名 */
        String routeName,

        /** 前端组件所属模块名，如 system */
        String moduleName,

        /** 按钮权限前缀使用的表名，如 sys_user */
        String tableName
) {
}