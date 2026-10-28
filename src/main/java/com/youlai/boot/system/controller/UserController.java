package com.youlai.boot.system.controller;

import cn.idev.excel.EasyExcel;
import cn.idev.excel.ExcelWriter;
import com.youlai.boot.common.annotation.Log;
import com.youlai.boot.framework.web.repeatsubmit.RepeatSubmit;
import com.youlai.boot.common.enums.ActionTypeEnum;
import com.youlai.boot.common.enums.LogModuleEnum;
import com.youlai.boot.common.exception.BusinessException;
import com.youlai.boot.framework.security.util.SecurityUtils;
import com.youlai.boot.common.model.Option;
import com.youlai.boot.common.result.ExcelResult;
import com.youlai.boot.common.result.PageResult;
import com.youlai.boot.common.result.Result;
import com.youlai.boot.common.util.ExcelUtils;
import com.youlai.boot.system.service.UserImportReader;
import com.youlai.boot.system.model.form.EmailUpdateForm;
import com.youlai.boot.system.model.form.MobileUpdateForm;
import com.youlai.boot.system.model.form.PasswordUpdateForm;
import com.youlai.boot.system.model.form.PasswordResetForm;
import com.youlai.boot.system.model.form.PasswordVerifyForm;
import com.youlai.boot.system.model.form.UserForm;
import com.youlai.boot.system.model.form.UserImportForm;
import com.youlai.boot.system.model.form.UserProfileForm;
import com.youlai.boot.system.model.vo.UserExportVO;
import com.youlai.boot.system.model.query.UserQuery;
import com.youlai.boot.system.model.vo.CurrentUserVO;
import com.youlai.boot.system.model.vo.UserPageVO;
import com.youlai.boot.system.model.vo.UserProfileVO;
import com.youlai.boot.system.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 用户管理控制器.
 *
 * @author Ray.Hao
 * @since 4.3.1
 */
@Tag(name = "02.用户接口")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {


    private final UserService userService;

    @Operation(summary = "用户列表")
    @GetMapping
    @PreAuthorize("@ss.hasPerm('sys:user:list')")
    @Log(module = LogModuleEnum.USER, value = ActionTypeEnum.LIST)
    public PageResult<UserPageVO> getUserList(
            @Valid UserQuery queryParams
    ) {
        return PageResult.success(userService.getUserPage(queryParams));
    }

    @Operation(summary = "新增用户")
    @PostMapping
    @PreAuthorize("@ss.hasPerm('sys:user:create')")
    @RepeatSubmit
    @Log(module = LogModuleEnum.USER, value = ActionTypeEnum.INSERT)
    public Result<Void> saveUser(
            @RequestBody @Valid UserForm userForm
    ) {
        boolean result = userService.saveUser(userForm);
        return Result.judge(result);
    }

    @Operation(summary = "获取用户表单数据")
    @GetMapping("/{userId}/form")
    @PreAuthorize("@ss.hasPerm('sys:user:update')")
    public Result<UserForm> getUserForm(
            @Parameter(description = "用户ID") @PathVariable Long userId
    ) {
        UserForm formData = userService.getUserFormData(userId);
        return Result.success(formData);
    }

    @Operation(summary = "修改用户")
    @PutMapping(value = "/{userId}")
    @PreAuthorize("@ss.hasPerm('sys:user:update')")
    @Log(module = LogModuleEnum.USER, value = ActionTypeEnum.UPDATE)
    public Result<Void> updateUser(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @RequestBody @Valid UserForm userForm
    ) {
        boolean result = userService.updateUser(userId, userForm);
        return Result.judge(result);
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('sys:user:delete')")
    @Log(module = LogModuleEnum.USER, value = ActionTypeEnum.DELETE)
    public Result<Void> deleteUsers(
            @Parameter(description = "用户ID，多个以英文逗号(,)分割") @PathVariable String ids
    ) {
        boolean result = userService.deleteUsers(ids);
        return Result.judge(result);
    }

    /**
     * 修改用户状态，仅接受0或1
     */
    @Operation(summary = "修改用户状态")
    @PatchMapping(value = "/{userId}/status")
    @PreAuthorize("@ss.hasPerm('sys:user:update')")
    @Log(module = LogModuleEnum.USER, value = ActionTypeEnum.UPDATE)
    public Result<Void> updateUserStatus(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @Parameter(description = "用户状态(1:启用;0:禁用)") @RequestParam
            @Min(value = 0, message = "用户状态只能为0或1")
            @Max(value = 1, message = "用户状态只能为0或1") Integer status
    ) {
        boolean result = userService.updateUserStatus(userId, status);
        return Result.judge(result);
    }

    @Operation(summary = "重置指定用户密码")
    @PutMapping(value = "/{userId}/password/reset")
    @PreAuthorize("@ss.hasPerm('sys:user:reset-password')")
    @Log(module = LogModuleEnum.USER, value = ActionTypeEnum.RESET_PASSWORD)
    public Result<Void> resetUserPassword(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @Parameter(description = "重置密码表单") @RequestBody @Valid PasswordResetForm formData
    ) {
        boolean result = userService.resetUserPassword(userId, formData.getPassword());
        return Result.judge(result);
    }

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/me")
    public Result<CurrentUserVO> getCurrentUser() {
        CurrentUserVO currentUserVo = userService.getCurrentUserInfo();
        return Result.success(currentUserVo);
    }

    @Operation(summary = "用户导入模板下载")
    @GetMapping("/template")
    public void downloadTemplate(HttpServletResponse response)  {
        String fileName = "用户导入模板.xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

        // ClassLoader 只认 / 分隔，用 File.separator 打 jar 后（Linux）会取不到资源
        String fileClassPath = "excel/" + fileName;
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(fileClassPath);

        try (ServletOutputStream outputStream = response.getOutputStream();
             ExcelWriter excelWriter = EasyExcel.write(outputStream).withTemplate(inputStream).build()) {
            excelWriter.finish();
        } catch (IOException e) {
            throw new BusinessException("用户导入模板下载失败", e);
        }
    }

    @Operation(summary = "导入用户")
    @PostMapping("/import")
    @PreAuthorize("@ss.hasPerm('sys:user:import')")
    @Log(module = LogModuleEnum.USER, value = ActionTypeEnum.IMPORT)
    public Result<ExcelResult> importUsers(MultipartFile file) throws IOException {
        UserImportReader reader = new UserImportReader();
        ExcelUtils.importExcel(file.getInputStream(), UserImportForm.class, reader);
        return Result.success(reader.getExcelResult());
    }

    @Operation(summary = "导出用户")
    @GetMapping("/export")
    @PreAuthorize("@ss.hasPerm('sys:user:export')")
    @Log(module = LogModuleEnum.USER, value = ActionTypeEnum.EXPORT)
    public void exportUsers(@Valid UserQuery queryParams, HttpServletResponse response) throws IOException {
        List<UserExportVO> exportUserList = userService.listExportUsers(queryParams);
        String fileName = "用户列表.xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

        EasyExcel.write(response.getOutputStream(), UserExportVO.class).sheet("用户列表")
                .doWrite(exportUserList);
    }

    @Operation(summary = "获取用户下拉选项")
    @GetMapping("/options")
    public Result<List<Option<String>>> listUserOptions() {
        List<Option<String>> list = userService.listUserOptions();
        return Result.success(list);
    }

    @Operation(summary = "获取个人中心用户信息")
    @GetMapping("/profile")
    public Result<UserProfileVO> getUserProfile() {
        Long userId = SecurityUtils.getUserId();
        UserProfileVO userProfile = userService.getUserProfile(userId);
        return Result.success(userProfile);
    }

    @Operation(summary = "个人中心修改用户信息")
    @PutMapping("/profile")
    @Log(module = LogModuleEnum.USER, value = ActionTypeEnum.UPDATE)
    public Result<Void> updateUserProfile(@RequestBody @Valid UserProfileForm formData) {
        boolean result = userService.updateUserProfile(formData);
        return Result.judge(result);
    }


    @Operation(summary = "当前用户修改密码")
    @PutMapping(value = "/password")
    @Log(module = LogModuleEnum.USER, value = ActionTypeEnum.CHANGE_PASSWORD)
    public Result<Void> changeCurrentUserPassword(
            @RequestBody @Valid PasswordUpdateForm data
    ) {
        Long currUserId = SecurityUtils.getUserId();
        boolean result = userService.changeUserPassword(currUserId, data);
        return Result.judge(result);
    }

    @Operation(summary = "发送短信验证码（绑定或更换手机号）")
    @PostMapping(value = "/mobile/code")
    public Result<Void> sendMobileCode(
            @Parameter(description = "手机号码", required = true) @RequestParam String mobile
    ) {
        boolean result = userService.sendMobileCode(mobile);
        return Result.judge(result);
    }

    @Operation(summary = "绑定或更换手机号")
    @PutMapping(value = "/mobile")
    public Result<Void> bindOrChangeMobile(
            @RequestBody @Validated MobileUpdateForm data
    ) {
        boolean result = userService.bindOrChangeMobile(data);
        return Result.judge(result);
    }

    @Operation(summary = "解绑手机号")
    @DeleteMapping(value = "/mobile")
    public Result<Void> unbindMobile(
            @RequestBody @Validated PasswordVerifyForm data
    ) {
        boolean result = userService.unbindMobile(data);
        return Result.judge(result);
    }

    @Operation(summary = "发送邮箱验证码（绑定或更换邮箱）")
    @PostMapping(value = "/email/code")
    public Result<Void> sendEmailCode(
            @Parameter(description = "邮箱地址", required = true) @RequestParam String email
    ) {
        userService.sendEmailCode(email);
        return Result.success();
    }

    @Operation(summary = "绑定或更换邮箱")
    @PutMapping(value = "/email")
    public Result<Void> bindOrChangeEmail(
            @RequestBody @Validated EmailUpdateForm data
    ) {
        boolean result = userService.bindOrChangeEmail(data);
        return Result.judge(result);
    }

    @Operation(summary = "解绑邮箱")
    @DeleteMapping(value = "/email")
    public Result<Void> unbindEmail(
            @RequestBody @Validated PasswordVerifyForm data
    ) {
        boolean result = userService.unbindEmail(data);
        return Result.judge(result);
    }

}
