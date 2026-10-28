package com.youlai.boot.framework.mybatis;

import com.baomidou.mybatisplus.core.metadata.OrderItem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 将公开排序属性转换为资源允许的数据库列
 *
 * @author Ray.Hao
 * @since 4.13.0
 */
public final class QuerySort {

    private static final int MAX_SORT_ITEMS = 3;

    private QuerySort() {
    }

    /**
     * 按输入顺序解析排序；未提供时返回空列表，由资源决定默认顺序
     */
    public static List<OrderItem> parse(String[] expressions, Map<String, String> fields) {
        List<OrderItem> orders = new ArrayList<>();
        if (expressions == null) {
            return orders;
        }
        if (expressions.length == 0 || expressions.length > MAX_SORT_ITEMS) {
            throw new IllegalArgumentException("排序规则应为 1 至 3 项");
        }
        Set<String> seen = new HashSet<>();
        for (String expression : expressions) {
            String[] parts = expression == null ? new String[0] : expression.split(",", -1);
            if (parts.length != 2 || !("asc".equals(parts[1]) || "desc".equals(parts[1]))) {
                throw new IllegalArgumentException("排序格式应为 字段,asc 或 字段,desc");
            }
            String field = parts[0];
            String column = fields.get(field);
            if (column == null) {
                throw new IllegalArgumentException("不支持的排序字段：" + field);
            }
            if (!seen.add(field)) {
                throw new IllegalArgumentException("排序字段重复：" + field);
            }
            orders.add("asc".equals(parts[1]) ? OrderItem.asc(column) : OrderItem.desc(column));
        }
        return orders;
    }
}
