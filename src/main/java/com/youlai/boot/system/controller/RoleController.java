package com.youlai.boot.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.common.enums.LogModuleEnum;
import com.youlai.boot.common.annotation.RepeatSubmit;
import com.youlai.boot.common.model.Option;
import com.youlai.boot.core.web.PageResult;
import com.youlai.boot.core.web.Result;
import com.youlai.boot.system.model.form.RoleForm;
import com.youlai.boot.system.model.query.RoleQuery;
import com.youlai.boot.system.model.vo.RolePageVO;
import com.youlai.boot.common.annotation.Log;
import com.youlai.boot.system.service.RoleService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.List;

/**
 * 角色控制层
 *
 * @author Ray.Hao
 * @since 2022/10/16
 */
@Tag(name = "03.角色接口")
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "角色分页列表")
    @GetMapping
    @Log(value = "角色分页列表", module = LogModuleEnum.ROLE)
    public PageResult<RolePageVO> getRolePage(
            RoleQuery queryParams
    ) {
        Page<RolePageVO> result = roleService.getRolePage(queryParams);
        return PageResult.success(result);
    }

    @Operation(summary = "角色下拉列表")
    @GetMapping("/options")
    public Result<List<Option<Long>>> listRoleOptions() {
        List<Option<Long>> list = roleService.listRoleOptions();
        return Result.success(list);
    }

    @Operation(summary = "新增角色")
    @PostMapping
    @PreAuthorize("@ss.hasPerm('sys:role:create')")
    @RepeatSubmit
    public Result<?> addRole(@Valid @RequestBody RoleForm roleForm) {
        boolean result = roleService.saveRole(roleForm);
        return Result.judge(result);
    }

    @Operation(summary = "获取角色表单数据")
    @GetMapping("/{roleId}/form")
    @PreAuthorize("@ss.hasPerm('sys:role:update')")
    public Result<RoleForm> getRoleForm(
            @Parameter(description = "角色ID") @PathVariable Long roleId
    ) {
        RoleForm roleForm = roleService.getRoleForm(roleId);
        return Result.success(roleForm);
    }

    @Operation(summary = "修改角色")
    @PutMapping(value = "/{id}")
    @PreAuthorize("@ss.hasPerm('sys:role:update')")
    public Result<?> updateRole(@Valid @RequestBody RoleForm roleForm) {
        boolean result = roleService.saveRole(roleForm);
        return Result.judge(result);
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('sys:role:delete')")
    public Result<Void> deleteRoles(
            @Parameter(description = "删除角色，多个以英文逗号(,)拼接") @PathVariable String ids
    ) {
        roleService.deleteRoles(ids);
        return Result.success();
    }

    @Operation(summary = "修改角色状态")
    @PutMapping(value = "/{roleId}/status")
    @PreAuthorize("@ss.hasPerm('sys:role:update')")
    public Result<?> updateRoleStatus(
            @Parameter(description = "角色ID") @PathVariable Long roleId,
            @Parameter(description = "状态(1:启用;0:禁用)") @RequestParam Integer status
    ) {
        boolean result = roleService.updateRoleStatus(roleId, status);
        return Result.judge(result);
    }

    @Operation(summary = "获取角色的菜单ID集合")
    @GetMapping("/{roleId}/menu-ids")
    public Result<List<Long>> getRoleMenuIds(
            @Parameter(description = "角色ID") @PathVariable Long roleId
    ) {
        List<Long> menuIds = roleService.getRoleMenuIds(roleId);
        return Result.success(menuIds);
    }

    @Operation(summary = "角色分配菜单权限")
    @PutMapping("/{roleId}/menus")
    @PreAuthorize("@ss.hasPerm('sys:role:assign')")
    public Result<Void> assignMenusToRole(
            @PathVariable Long roleId,
            @RequestBody List<Long> menuIds
    ) {
        roleService.assignMenusToRole(roleId, menuIds);
        return Result.success();
    }

    @Operation(summary = "获取角色的部门ID集合(自定义数据权限)")
    @GetMapping("/{roleId}/dept-ids")
    @PreAuthorize("@ss.hasPerm('sys:role:update')")
    public Result<List<Long>> getRoleDeptIds(
            @Parameter(description = "角色ID") @PathVariable Long roleId
    ) {
        List<Long> deptIds = roleService.getRoleDeptIds(roleId);
        return Result.success(deptIds);
    }
}
