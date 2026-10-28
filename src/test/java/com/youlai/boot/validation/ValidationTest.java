package com.youlai.boot.validation;

import com.youlai.boot.codegen.model.form.GenConfigForm;
import com.youlai.boot.module.form.model.form.FormDefinitionForm;
import com.youlai.boot.system.model.form.PasswordResetForm;
import com.youlai.boot.system.model.form.PasswordUpdateForm;
import com.youlai.boot.system.model.query.UserQuery;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 参数校验约束测试
 *
 * @author Ray.Hao
 * @since 4.8.0
 */
class ValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    @DisplayName("密码重置要求非空且长度达标")
    void passwordResetFormShouldRejectBlankAndShortPassword() {
        var blank = new PasswordResetForm();
        assertThat(validator.validate(blank)).isNotEmpty();

        var form = new PasswordResetForm();
        form.setPassword("12345");
        assertThat(validator.validate(form)).isNotEmpty();

        form.setPassword("123456");
        assertThat(validator.validate(form)).isEmpty();
    }

    @Test
    @DisplayName("修改密码三个字段都必填")
    void passwordUpdateFormShouldRequireAllThreeFields() {
        var form = new PasswordUpdateForm();
        assertThat(validator.validate(form)).hasSize(3);

        form.setOldPassword("123456");
        form.setNewPassword("123456");
        form.setConfirmPassword("123456");
        assertThat(validator.validate(form)).isEmpty();
    }

    @Test
    @DisplayName("分页参数超出范围被拒绝")
    void baseQueryShouldRejectOutOfRangePaging() {
        var query = new UserQuery();
        assertThat(validator.validate(query)).isEmpty();

        query.setPageNum(0);
        assertThat(validator.validate(query)).isNotEmpty();

        query.setPageNum(1);
        query.setPageSize(201);
        assertThat(validator.validate(query)).isNotEmpty();

        query.setPageSize(200);
        assertThat(validator.validate(query)).isEmpty();
    }

    @Test
    @DisplayName("表单定义按创建与更新分组校验")
    void formDefinitionFormShouldValidateByCreateAndUpdateGroups() {
        var form = new FormDefinitionForm();
        // 创建：标识与名称为必填
        assertThat(validator.validate(form, FormDefinitionForm.Create.class)).hasSize(2);

        form.setFormKey("EmployeeOnboarding");
        form.setFormName("入职信息采集");
        // 标识格式非法仍被拦截
        assertThat(validator.validate(form, FormDefinitionForm.Create.class)).hasSize(1);

        form.setFormKey("employee_onboarding");
        assertThat(validator.validate(form, FormDefinitionForm.Create.class)).isEmpty();

        // 更新：只保存规则的部分更新载荷不触发必填校验
        var ruleOnly = new FormDefinitionForm();
        ruleOnly.setFormJson("[]");
        ruleOnly.setOptionsJson("{}");
        assertThat(validator.validate(ruleOnly, FormDefinitionForm.Update.class)).isEmpty();

        // 更新：显式提交的字段格式仍被校验
        ruleOnly.setFormName("x".repeat(101));
        assertThat(validator.validate(ruleOnly, FormDefinitionForm.Update.class)).hasSize(1);
    }

    @Test
    @DisplayName("代码生成配置要求核心字段")
    void genConfigFormShouldRequireCoreFields() {
        var form = new GenConfigForm();
        assertThat(validator.validate(form)).isNotEmpty();

        form.setTableName("sys_user");
        form.setBusinessName("用户");
        form.setModuleName("system");
        form.setPackageName("com.youlai");
        form.setEntityName("User");
        assertThat(validator.validate(form)).isEmpty();
    }

    @Test
    @DisplayName("每个 @RequestBody 参数都必须触发校验")
    void everyRequestBodyParameterShouldTriggerValidation() throws Exception {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        List<String> violations = new ArrayList<>();
        for (var definition : scanner.findCandidateComponents("com.youlai.boot")) {
            Class<?> controller = Class.forName(definition.getBeanClassName());
            for (Method method : controller.getDeclaredMethods()) {
                for (Parameter parameter : method.getParameters()) {
                    // Bean Validation 只作用于 Bean 类型，Map/List 等动态数据由 Service 层业务校验
                    boolean isBeanType = parameter.getType().getName().startsWith("com.youlai.boot");
                    boolean missingValidation = isBeanType
                            && parameter.isAnnotationPresent(RequestBody.class)
                            && !parameter.isAnnotationPresent(Valid.class)
                            && !parameter.isAnnotationPresent(Validated.class);
                    if (missingValidation) {
                        violations.add(controller.getSimpleName() + "#" + method.getName()
                                + " 的参数 " + parameter.getName() + " 缺少 @Valid");
                    }
                }
            }
        }
        assertThat(violations).as(String.join("；", violations)).isEmpty();
    }
}
