package com.youlai.boot.plugin.mybatis;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.extension.plugins.handler.DataPermissionHandler;
import com.youlai.boot.common.annotation.DataPermission;
import com.youlai.boot.common.enums.DataScopeEnum;
import com.youlai.boot.security.model.RoleDataScope;
import com.youlai.boot.security.util.SecurityUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SubSelect;

import java.lang.reflect.Method;
import java.util.List;

/**
 * 数据权限控制器
 * <p>
 * 支持多角色数据权限合并（并集策略）：
 * - 如果任一角色是 ALL，则跳过数据权限过滤
 * - 否则用 OR 连接各角色的数据权限条件
 * <p>
 * 使用 JSQLParser 构建 SQL 条件，避免字符串拼接，提高代码安全性和可读性。
 *
 * @author zc
 * @since 2021-12-10 13:28
 */
@Slf4j
public class MyDataPermissionHandler implements DataPermissionHandler {

    private static final String DEPT_TABLE = "sys_dept";
    private static final String DEPT_ID_COLUMN = "id";
    private static final String DEPT_TREE_PATH_COLUMN = "tree_path";

    /**
     * 获取数据权限的sql片段
     *
     * @param where             查询条件
     * @param mappedStatementId mapper接口方法的全路径
     * @return sql片段
     */
    @Override
    @SneakyThrows
    public Expression getSqlSegment(Expression where, String mappedStatementId) {
        // 如果是未登录，或者是定时任务执行的SQL，或者是超级管理员，直接返回
        if (SecurityUtils.getUserId() == null || SecurityUtils.isRoot()) {
            return where;
        }

        // 获取当前用户的数据权限列表
        List<RoleDataScope> dataScopes = SecurityUtils.getUser()
                .map(user -> user.getDataScopes())
                .orElse(List.of());

        // 如果任一角色是 ALL，则跳过数据权限过滤（并集策略）
        if (hasAllDataScope(dataScopes)) {
            return where;
        }

        // 如果没有数据权限，跳过过滤
        if (CollectionUtil.isEmpty(dataScopes)) {
            return where;
        }

        // 获取当前执行的接口类
        Class<?> clazz = Class.forName(mappedStatementId.substring(0, mappedStatementId.lastIndexOf(StringPool.DOT)));
        // 获取当前执行的方法名称
        String methodName = mappedStatementId.substring(mappedStatementId.lastIndexOf(StringPool.DOT) + 1);
        // 获取当前执行的接口类里所有的方法
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            // 找到当前执行的方法
            if (method.getName().equals(methodName)) {
                DataPermission annotation = method.getAnnotation(DataPermission.class);
                // 判断当前执行的方法是否有权限注解，如果没有注解直接返回
                if (annotation == null) {
                    return where;
                }
                // 使用并集策略过滤
                return dataScopeFilterWithUnion(annotation, dataScopes, where);
            }
        }
        return where;
    }

    /**
     * 判断是否包含"全部数据"权限
     *
     * @param dataScopes 数据权限列表
     * @return 是否有全部数据权限
     */
    private boolean hasAllDataScope(List<RoleDataScope> dataScopes) {
        if (CollectionUtil.isEmpty(dataScopes)) {
            return false;
        }
        return dataScopes.stream()
                .anyMatch(scope -> DataScopeEnum.ALL.getValue().equals(scope.getDataScope()));
    }

    /**
     * 使用并集策略进行数据权限过滤
     * <p>
     * 多个角色的数据权限通过 OR 连接，实现并集效果
     *
     * @param annotation  数据权限注解
     * @param dataScopes  数据权限列表
     * @param where       原始查询条件
     * @return 追加权限过滤后的查询条件
     */
    private Expression dataScopeFilterWithUnion(DataPermission annotation, List<RoleDataScope> dataScopes, Expression where) {
        String deptAlias = annotation.deptAlias();
        String deptIdColumnName = annotation.deptIdColumnName();
        String userAlias = annotation.userAlias();
        String userIdColumnName = annotation.userIdColumnName();

        // 构建各角色的数据权限条件，使用 OR 连接实现并集
        Expression unionExpression = null;
        for (RoleDataScope dataScope : dataScopes) {
            Expression roleExpression = buildRoleDataScopeExpression(
                    deptAlias, deptIdColumnName, userAlias, userIdColumnName, dataScope);
            if (roleExpression != null) {
                if (unionExpression == null) {
                    unionExpression = roleExpression;
                } else {
                    // 使用 OR 连接各角色的条件（并集）
                    unionExpression = new OrExpression(unionExpression, roleExpression);
                }
            }
        }

        if (unionExpression == null) {
            return where;
        }

        // 用括号包裹并集条件
        Expression finalExpression = new Parenthesis(unionExpression);

        if (where == null) {
            return finalExpression;
        }

        return new AndExpression(where, finalExpression);
    }

    /**
     * 构建单个角色的数据权限SQL条件
     * <p>
     * 使用 JSQLParser 构建 Expression，避免字符串拼接
     *
     * @param deptAlias        部门表别名
     * @param deptIdColumnName 部门ID字段名
     * @param userAlias        用户表别名
     * @param userIdColumnName 用户ID字段名
     * @param roleDataScope    角色数据权限
     * @return 数据权限条件表达式
     */
    private Expression buildRoleDataScopeExpression(String deptAlias, String deptIdColumnName,
                                                     String userAlias, String userIdColumnName,
                                                     RoleDataScope roleDataScope) {
        Column deptColumn = buildColumn(deptAlias, deptIdColumnName);
        Column userColumn = buildColumn(userAlias, userIdColumnName);

        Long deptId = SecurityUtils.getDeptId();
        Long userId = SecurityUtils.getUserId();

        DataScopeEnum dataScopeEnum = DataScopeEnum.getByValue(roleDataScope.getDataScope());
        if (dataScopeEnum == null) {
            return null;
        }

        return switch (dataScopeEnum) {
            case ALL -> null; // 全部数据权限，不添加过滤条件
            case DEPT_AND_SUB -> buildDeptAndSubExpression(deptColumn, deptId);
            case DEPT -> buildEqualsExpression(deptColumn, deptId);
            case SELF -> buildEqualsExpression(userColumn, userId);
            case CUSTOM -> buildCustomDeptExpression(deptColumn, roleDataScope.getCustomDeptIds());
        };
    }

    /**
     * 构建列引用
     *
     * @param alias      表别名
     * @param columnName 列名
     * @return 列引用
     */
    private Column buildColumn(String alias, String columnName) {
        if (StrUtil.isNotBlank(alias)) {
            return new Column(alias + StringPool.DOT + columnName);
        }
        return new Column(columnName);
    }

    /**
     * 构建等于条件
     *
     * @param column 列
     * @param value  值
     * @return 等于表达式
     */
    private Expression buildEqualsExpression(Column column, Long value) {
        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(column);
        equalsTo.setRightExpression(new LongValue(value));
        return equalsTo;
    }

    /**
     * 构建部门及子部门数据权限条件
     * <p>
     * SQL: dept_id IN (SELECT id FROM sys_dept WHERE id = ? OR FIND_IN_SET(?, tree_path))
     *
     * @param deptColumn 部门列
     * @param deptId     部门ID
     * @return IN 子查询表达式
     */
    private Expression buildDeptAndSubExpression(Column deptColumn, Long deptId) {
        // 构建子查询: SELECT id FROM sys_dept WHERE id = ? OR FIND_IN_SET(?, tree_path)
        PlainSelect subSelectBody = new PlainSelect();
        subSelectBody.setFromItem(new Table(DEPT_TABLE));
        subSelectBody.addSelectItems(new Column(DEPT_ID_COLUMN));

        // WHERE id = ?
        EqualsTo idEquals = new EqualsTo();
        idEquals.setLeftExpression(new Column(DEPT_ID_COLUMN));
        idEquals.setRightExpression(new LongValue(deptId));

        // FIND_IN_SET(?, tree_path)
        Function findInSet = new Function();
        findInSet.setName("FIND_IN_SET");
        findInSet.setParameters(new ExpressionList<>(
                new LongValue(deptId),
                new Column(DEPT_TREE_PATH_COLUMN)
        ));

        // WHERE id = ? OR FIND_IN_SET(?, tree_path)
        OrExpression whereClause = new OrExpression(idEquals, findInSet);
        subSelectBody.setWhere(whereClause);

        // 构建子查询
        SubSelect subSelect = new SubSelect();
        subSelect.setSelectBody(subSelectBody);

        // 构建 IN 表达式
        return new InExpression(deptColumn, subSelect);
    }

    /**
     * 构建自定义部门数据权限条件
     * <p>
     * SQL: dept_id IN (?, ?, ...)
     *
     * @param deptColumn    部门列
     * @param customDeptIds 自定义部门ID列表
     * @return IN 表达式，如果没有部门则返回 1=0
     */
    private Expression buildCustomDeptExpression(Column deptColumn, List<Long> customDeptIds) {
        if (CollectionUtil.isEmpty(customDeptIds)) {
            // 没有自定义部门，返回 1=0（无权限）
            EqualsTo falseCondition = new EqualsTo();
            falseCondition.setLeftExpression(new LongValue(1));
            falseCondition.setRightExpression(new LongValue(0));
            return falseCondition;
        }

        // 构建 IN 表达式列表
        ExpressionList<Expression> deptIdList = new ExpressionList<>();
        for (Long deptId : customDeptIds) {
            deptIdList.addExpression(new LongValue(deptId));
        }

        return new InExpression(deptColumn, deptIdList);
    }

}
