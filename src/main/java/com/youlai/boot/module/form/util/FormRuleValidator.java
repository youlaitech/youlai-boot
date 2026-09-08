package com.youlai.boot.module.form.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youlai.boot.common.exception.BusinessException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 表单规则校验器（后端基础校验 + 字段白名单）
 *
 * <p>按 form-create rule 的 validate 声明校验必填（required）与基础类型（number/array），
 * 并按 rule 声明的 field 过滤数据</p>
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
public final class FormRuleValidator {

    /**
     * Jackson 线程安全，静态复用
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private FormRuleValidator() {
    }

    /**
     * 校验并过滤表单数据
     *
     * <p>白名单：仅保留 rule 中声明 field 的数据项；
     * 校验：逐字段执行 required/类型校验，任一失败抛业务异常并列出全部错误</p>
     *
     * @param formJson 表单规则（form-create rule 数组 JSON 字符串）
     * @param data     提交数据（field -> value 映射）
     * @return 白名单过滤后的数据（保持 rule 字段顺序）
     */
    public static Map<String, Object> validateAndFilter(String formJson, Map<String, Object> data) {
        JsonNode rules;
        try {
            rules = OBJECT_MAPPER.readTree(formJson);
        } catch (Exception e) {
            throw new BusinessException("表单规则解析失败");
        }
        if (!rules.isArray()) {
            throw new BusinessException("表单规则格式非法");
        }

        Map<String, Object> filtered = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        walk(rules, data, filtered, errors);
        if (!errors.isEmpty()) {
            throw new BusinessException(String.join("；", errors));
        }
        return filtered;
    }

    /**
     * 递归遍历规则节点：布局容器（row/col）自身无 field，其 children 递归收集
     *
     * @param nodes    规则节点数组
     * @param data     提交数据
     * @param filtered 白名单过滤结果（累积写入）
     * @param errors   校验错误信息（累积写入）
     */
    private static void walk(JsonNode nodes, Map<String, Object> data,
                             Map<String, Object> filtered, List<String> errors) {
        for (JsonNode node : nodes) {
            if (node == null || !node.isObject()) {
                continue;
            }
            JsonNode fieldNode = node.get("field");
            if (fieldNode != null && fieldNode.isTextual() && !fieldNode.asText().isEmpty()) {
                String field = fieldNode.asText();
                // 字段白名单：rule 未声明的 field 直接丢弃
                if (data.containsKey(field)) {
                    filtered.put(field, data.get(field));
                }
                validateField(node, field, data.get(field), errors);
            }
            JsonNode children = node.get("children");
            if (children != null && children.isArray()) {
                walk(children, data, filtered, errors);
            }
        }
    }

    /**
     * 单字段校验：必填与基础类型
     *
     * @param ruleNode 字段规则节点
     * @param field    字段名
     * @param value    提交值（可能为 null）
     * @param errors   校验错误信息（累积写入）
     */
    private static void validateField(JsonNode ruleNode, String field, Object value, List<String> errors) {
        JsonNode validateArray = ruleNode.get("validate");
        if (validateArray == null || !validateArray.isArray()) {
            return;
        }
        String title = resolveTitle(ruleNode, field);
        for (JsonNode validate : validateArray) {
            // 必填：null/空串/空集合均视为未填写
            if (validate.path("required").asBoolean(false) && isEmpty(value)) {
                errors.add(requiredMessage(validate, title));
            }
            // 类型：仅值非空时校验，避免与 required 错误重复
            if (!isEmpty(value)) {
                String type = validate.path("type").asText("");
                if (!checkType(type, value)) {
                    errors.add("「" + title + "」数据类型非法");
                }
            }
        }
    }

    /**
     * 解析字段标题（错误提示用），缺省回退字段名
     */
    private static String resolveTitle(JsonNode ruleNode, String field) {
        JsonNode title = ruleNode.get("title");
        return title != null && title.isTextual() && !title.asText().isEmpty() ? title.asText() : field;
    }

    /**
     * 必填错误消息：优先取 rule validate 里配置的 message，缺省用通用文案
     */
    private static String requiredMessage(JsonNode validate, String title) {
        JsonNode message = validate.get("message");
        return message != null && message.isTextual() && !message.asText().isEmpty()
                ? message.asText() : "「" + title + "」不能为空";
    }

    /**
     * 空值判定：null、空字符串、空集合
     *
     * @param value 提交值
     */
    private static boolean isEmpty(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String str) {
            return str.trim().isEmpty();
        }
        return value instanceof Collection<?> collection && collection.isEmpty();
    }

    /**
     * 基础类型校验：number（数字或数字串）/ array（集合），
     * string/date 等文本类型无可靠判据，不做强校验
     *
     * @param type  validate 规则声明的类型
     * @param value 提交值（非空）
     */
    private static boolean checkType(String type, Object value) {
        return switch (type) {
            case "number", "integer", "float" -> isNumeric(value);
            case "array" -> value instanceof Collection<?>;
            default -> true;
        };
    }

    /**
     * 数字判定：Number 实例或可解析为 double 的字符串
     *
     * @param value 提交值（非空）
     */
    private static boolean isNumeric(Object value) {
        if (value instanceof Number) {
            return true;
        }
        if (value instanceof String str) {
            try {
                Double.parseDouble(str.trim());
                return true;
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        return false;
    }

}
