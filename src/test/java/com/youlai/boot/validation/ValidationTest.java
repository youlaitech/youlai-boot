package com.youlai.boot.validation;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
    void passwordResetFormShouldRejectBlankAndShortPassword() {
        var blank = new com.youlai.boot.system.model.form.PasswordResetForm();
        assertThat(validator.validate(blank)).isNotEmpty();

        var form = new com.youlai.boot.system.model.form.PasswordResetForm();
        form.setPassword("12345");
        assertThat(validator.validate(form)).isNotEmpty();

        form.setPassword("123456");
        assertThat(validator.validate(form)).isEmpty();
    }

    @Test
    void passwordUpdateFormShouldRequireAllThreeFields() {
        var form = new com.youlai.boot.system.model.form.PasswordUpdateForm();
        assertThat(validator.validate(form)).hasSize(3);

        form.setOldPassword("123456");
        form.setNewPassword("123456");
        form.setConfirmPassword("123456");
        assertThat(validator.validate(form)).isEmpty();
    }

    @Test
    void baseQueryShouldRejectOutOfRangePaging() {
        var query = new com.youlai.boot.system.model.query.UserQuery();
        assertThat(validator.validate(query)).isEmpty();

        query.setPageNum(0);
        assertThat(validator.validate(query)).isNotEmpty();

        query.setPageNum(1);
        query.setPageSize(201);
        assertThat(validator.validate(query)).isNotEmpty();

        query.setPageSize(200);
        query.setOrder("DROP TABLE");
        assertThat(validator.validate(query)).isNotEmpty();

        query.setOrder("DESC");
        assertThat(validator.validate(query)).isEmpty();
    }

    @Test
    void genConfigFormShouldRequireCoreFields() {
        var form = new com.youlai.boot.codegen.model.form.GenConfigForm();
        assertThat(validator.validate(form)).isNotEmpty();

        form.setTableName("sys_user");
        form.setBusinessName("用户");
        form.setModuleName("system");
        form.setPackageName("com.youlai");
        form.setEntityName("User");
        assertThat(validator.validate(form)).isEmpty();
    }

    @Test
    void everyRequestBodyParameterShouldTriggerValidation() throws Exception {
        var scanner = new org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider(false);
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
