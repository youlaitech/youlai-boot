package com.youlai.boot.system.controller;

import com.youlai.boot.common.annotation.Log;
import com.youlai.boot.framework.web.repeatsubmit.RepeatSubmit;
import com.youlai.boot.common.enums.ActionTypeEnum;
import com.youlai.boot.common.enums.LogModuleEnum;
import com.youlai.boot.common.model.Option;
import com.youlai.boot.common.result.Result;
import com.youlai.boot.system.model.form.MenuAiFillForm;
import com.youlai.boot.system.model.form.MenuForm;
import com.youlai.boot.system.model.query.MenuQuery;
import com.youlai.boot.system.model.vo.MenuAiVO;
import com.youlai.boot.system.model.vo.MenuVO;
import com.youlai.boot.system.model.vo.RouteVO;
import com.youlai.boot.system.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单控制层
 *
 * @author Ray.Hao
 * @since 2020/11/06
 */
@Tag(name = "04.菜单接口")
@RestController
@RequestMapping("/api/v1/menus")
@RequiredArgsConstructor
@Slf4j
public class MenuController {

    private final MenuService menuService;

    @Operation(summary = "菜单列表")
    @GetMapping
    @Log(module = LogModuleEnum.MENU, value = ActionTypeEnum.LIST)
    public Result<List<MenuVO>> getMenus(@Valid MenuQuery queryParams) {
        List<MenuVO> menuList = menuService.listMenus(queryParams);
        return Result.success(menuList);
    }

    @Operation(summary = "菜单下拉列表")
    @GetMapping("/options")
    public Result<List<Option<Long>>> getMenuOptions(
            @Parameter(description = "菜单类型，可传多个，为空查全部") @RequestParam(required = false) List<String> types) {
        return Result.success(menuService.listMenuOptions(types));
    }

    @Operation(summary = "当前用户菜单路由列表")
    @GetMapping("/routes")
    public Result<List<RouteVO>> getCurrentUserRoutes() {
        List<RouteVO> routeList = menuService.listCurrentUserRoutes();
        return Result.success(routeList);
    }

    @Operation(summary = "菜单表单数据")
    @GetMapping("/{menuId}/form")
    @PreAuthorize("@ss.hasPerm('sys:menu:update')")
    public Result<MenuForm> getMenuForm(
            @Parameter(description = "菜单ID") @PathVariable Long menuId
    ) {
        MenuForm menu = menuService.getMenuForm(menuId);
        return Result.success(menu);
    }

    @Operation(summary = "新增菜单")
    @PostMapping
    @PreAuthorize("@ss.hasPerm('sys:menu:create')")
    @RepeatSubmit
    @Log(module = LogModuleEnum.MENU, value = ActionTypeEnum.INSERT)
    public Result<Void> addMenu(@RequestBody @Valid MenuForm menuForm) {
        boolean result = menuService.saveMenu(menuForm);
        return Result.judge(result);
    }

    @Operation(summary = "修改菜单")
    @PutMapping(value = "/{menuId}")
    @PreAuthorize("@ss.hasPerm('sys:menu:update')")
    @Log(module = LogModuleEnum.MENU, value = ActionTypeEnum.UPDATE)
    public Result<Void> updateMenu(
            @Parameter(description = "菜单ID") @PathVariable Long menuId,
            @RequestBody @Valid MenuForm menuForm
    ) {
        boolean result = menuService.saveMenu(menuForm);
        return Result.judge(result);
    }

    @Operation(summary = "删除菜单")
    @DeleteMapping("/{menuId}")
    @PreAuthorize("@ss.hasPerm('sys:menu:delete')")
    @Log(module = LogModuleEnum.MENU, value = ActionTypeEnum.DELETE)
    public Result<Void> deleteMenu(
            @Parameter(description = "菜单ID，多个以英文(,)分割") @PathVariable Long menuId
    ) {
        boolean result = menuService.deleteMenu(menuId);
        return Result.judge(result);
    }

    @Operation(summary = "修改菜单显示状态")
    @PatchMapping("/{menuId}")
    @PreAuthorize("@ss.hasPerm('sys:menu:update')")
    @Log(module = LogModuleEnum.MENU, value = ActionTypeEnum.UPDATE)
    public Result<Void> updateMenuVisible(
            @Parameter(description = "菜单ID") @PathVariable Long menuId,
            @Parameter(description = "显示状态(1:显示;0:隐藏)") Integer visible

    ) {
        boolean result = menuService.updateMenuVisible(menuId, visible);
        return Result.judge(result);
    }

    // AI 辅助：未开启 AI 时调用返回业务异常

    @Operation(summary = "AI 推断菜单的访问路径与权限标识")
    @PostMapping("/ai-fill")
    public Result<MenuAiVO> aiFillMenu(@RequestBody @Valid MenuAiFillForm form) {
        return Result.success(menuService.aiFillMenu(form));
    }

}

