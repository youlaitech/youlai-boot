package com.youlai.boot.plugin.mybatis;

import com.youlai.boot.common.annotation.DataPermission;
import com.youlai.boot.common.enums.DataScopeEnum;
import com.youlai.boot.security.model.RoleDataScope;
import com.youlai.boot.security.model.SysUserDetails;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.schema.Column;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 数据权限处理器单元测试
 *
 * @author Ray.Hao
 */
@DisplayName("数据权限处理器测试")
class MyDataPermissionHandlerTest {

    private MyDataPermissionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new MyDataPermissionHandler();
    }

    @AfterEach
    void tearDown() {
        // 清理安全上下文
        SecurityContextHolder.clearContext();
    }

    // ==================== 边界条件测试 ====================

    @Nested
    @DisplayName("边界条件测试")
    class BoundaryTests {

        @Test
        @DisplayName("未登录用户 - 返回原始where条件")
        void whenNotLoggedIn_thenReturnOriginalWhere() {
            // given: 未设置安全上下文
            Expression where = new EqualsTo(new Column("status"), new net.sf.jsqlparser.expression.LongValue(1));

            // when
            Expression result = handler.getSqlSegment(where, "com.example.mapper.UserMapper.selectList");

            // then: 返回原始where
            assertThat(result).isSameAs(where);
        }

        @Test
        @DisplayName("超级管理员 - 跳过数据权限过滤")
        void whenRootUser_thenReturnOriginalWhere() {
            // given: 设置超级管理员
            setSecurityContext(1L, "admin", 1L, 
                    Set.of(new SimpleGrantedAuthority("ROLE_ROOT")),
                    Collections.emptyList());

            Expression where = new EqualsTo(new Column("status"), new net.sf.jsqlparser.expression.LongValue(1));

            // when
            Expression result = handler.getSqlSegment(where, "com.example.mapper.UserMapper.selectList");

            // then: 返回原始where，不添加数据权限条件
            assertThat(result).isSameAs(where);
        }

        @Test
        @DisplayName("无数据权限列表 - 返回原始where条件")
        void whenNoDataScopes_thenReturnOriginalWhere() {
            // given: 普通用户但无数据权限
            setSecurityContext(100L, "user", 10L,
                    Set.of(new SimpleGrantedAuthority("ROLE_USER")),
                    Collections.emptyList());

            Expression where = new EqualsTo(new Column("status"), new net.sf.jsqlparser.expression.LongValue(1));

            // when
            Expression result = handler.getSqlSegment(where, "com.example.mapper.UserMapper.selectList");

            // then: 返回原始where
            assertThat(result).isSameAs(where);
        }
    }

    // ==================== 单一角色数据权限测试 ====================

    @Nested
    @DisplayName("单一角色数据权限测试")
    class SingleRoleTests {

        @Test
        @DisplayName("全部数据权限(ALL) - 不添加过滤条件")
        void whenAllDataScope_thenReturnOriginalWhere() {
            // given: 角色拥有全部数据权限
            setSecurityContext(100L, "admin", 10L,
                    Set.of(new SimpleGrantedAuthority("ROLE_ADMIN")),
                    List.of(RoleDataScope.all("ADMIN")));

            Expression where = new EqualsTo(new Column("status"), new net.sf.jsqlparser.expression.LongValue(1));

            // when
            Expression result = handler.getSqlSegment(where, 
                    "com.youlai.boot.plugin.mybatis.TestDataPermissionMapper.selectList");

            // then: 返回原始where
            assertThat(result).isSameAs(where);
        }

        @Test
        @DisplayName("本部门数据权限(DEPT) - 添加dept_id = ?条件")
        void whenDeptDataScope_thenAddDeptIdCondition() {
            // given: 角色拥有本部门数据权限
            Long deptId = 10L;
            setSecurityContext(100L, "manager", deptId,
                    Set.of(new SimpleGrantedAuthority("ROLE_MANAGER")),
                    List.of(RoleDataScope.dept("MANAGER")));

            Expression where = null;

            // when
            Expression result = handler.getSqlSegment(where,
                    "com.youlai.boot.plugin.mybatis.TestDataPermissionMapper.selectList");

            // then: 添加部门过滤条件
            assertThat(result).isNotNull();
            assertThat(result.toString()).contains("dept_id = " + deptId);
        }

        @Test
        @DisplayName("本人数据权限(SELF) - 添加create_by = ?条件")
        void whenSelfDataScope_thenAddCreateByCondition() {
            // given: 角色拥有本人数据权限
            Long userId = 100L;
            setSecurityContext(userId, "user", 10L,
                    Set.of(new SimpleGrantedAuthority("ROLE_USER")),
                    List.of(RoleDataScope.self("USER")));

            Expression where = null;

            // when
            Expression result = handler.getSqlSegment(where,
                    "com.youlai.boot.plugin.mybatis.TestDataPermissionMapper.selectList");

            // then: 添加用户过滤条件
            assertThat(result).isNotNull();
            assertThat(result.toString()).contains("create_by = " + userId);
        }

        @Test
        @DisplayName("自定义部门数据权限(CUSTOM) - 添加dept_id IN (?)条件")
        void whenCustomDataScope_thenAddDeptIdInCondition() {
            // given: 角色拥有自定义部门数据权限
            List<Long> customDeptIds = Arrays.asList(10L, 20L, 30L);
            setSecurityContext(100L, "user", 10L,
                    Set.of(new SimpleGrantedAuthority("ROLE_CUSTOM")),
                    List.of(RoleDataScope.custom("CUSTOM", customDeptIds)));

            Expression where = null;

            // when
            Expression result = handler.getSqlSegment(where,
                    "com.youlai.boot.plugin.mybatis.TestDataPermissionMapper.selectList");

            // then: 添加自定义部门IN条件
            assertThat(result).isNotNull();
            String sql = result.toString();
            assertThat(sql).contains("dept_id IN");
            assertThat(sql).contains("10");
            assertThat(sql).contains("20");
            assertThat(sql).contains("30");
        }

        @Test
        @DisplayName("自定义部门数据权限(空列表) - 添加1=0条件")
        void whenCustomDataScopeWithEmptyList_thenAddFalseCondition() {
            // given: 角色拥有自定义部门权限但列表为空
            setSecurityContext(100L, "user", 10L,
                    Set.of(new SimpleGrantedAuthority("ROLE_CUSTOM")),
                    List.of(RoleDataScope.custom("CUSTOM", Collections.emptyList())));

            Expression where = null;

            // when
            Expression result = handler.getSqlSegment(where,
                    "com.youlai.boot.plugin.mybatis.TestDataPermissionMapper.selectList");

            // then: 添加1=0条件（无权限）
            assertThat(result).isNotNull();
            assertThat(result.toString()).contains("1 = 0");
        }

        @Test
        @DisplayName("部门及子部门数据权限(DEPT_AND_SUB) - 添加子查询条件")
        void whenDeptAndSubDataScope_thenAddSubQueryCondition() {
            // given: 角色拥有部门及子部门数据权限
            Long deptId = 10L;
            setSecurityContext(100L, "manager", deptId,
                    Set.of(new SimpleGrantedAuthority("ROLE_MANAGER")),
                    List.of(RoleDataScope.deptAndSub("MANAGER")));

            Expression where = null;

            // when
            Expression result = handler.getSqlSegment(where,
                    "com.youlai.boot.plugin.mybatis.TestDataPermissionMapper.selectList");

            // then: 添加子查询条件
            assertThat(result).isNotNull();
            String sql = result.toString();
            assertThat(sql).contains("dept_id IN");
            assertThat(sql).contains("SELECT");
            assertThat(sql).contains("sys_dept");
            assertThat(sql).contains("FIND_IN_SET");
        }
    }

    // ==================== 多角色并集策略测试 ====================

    @Nested
    @DisplayName("多角色并集策略测试")
    class MultiRoleTests {

        @Test
        @DisplayName("多角色 - 任一角色为ALL时跳过过滤")
        void whenAnyRoleIsAll_thenSkipFilter() {
            // given: 用户有两个角色，其中一个是ALL
            setSecurityContext(100L, "user", 10L,
                    Set.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER")),
                    List.of(
                            RoleDataScope.all("ADMIN"),
                            RoleDataScope.self("USER")
                    ));

            Expression where = new EqualsTo(new Column("status"), new net.sf.jsqlparser.expression.LongValue(1));

            // when
            Expression result = handler.getSqlSegment(where,
                    "com.youlai.boot.plugin.mybatis.TestDataPermissionMapper.selectList");

            // then: 返回原始where，不添加过滤条件
            assertThat(result).isSameAs(where);
        }

        @Test
        @DisplayName("多角色 - DEPT和SELF权限合并为OR条件")
        void whenDeptAndSelfRoles_thenMergeWithOr() {
            // given: 用户有两个角色，分别拥有部门和本人权限
            Long deptId = 10L;
            Long userId = 100L;
            setSecurityContext(userId, "manager", deptId,
                    Set.of(new SimpleGrantedAuthority("ROLE_MANAGER"), new SimpleGrantedAuthority("ROLE_USER")),
                    List.of(
                            RoleDataScope.dept("MANAGER"),
                            RoleDataScope.self("USER")
                    ));

            Expression where = null;

            // when
            Expression result = handler.getSqlSegment(where,
                    "com.youlai.boot.plugin.mybatis.TestDataPermissionMapper.selectList");

            // then: 生成OR连接的合并条件
            assertThat(result).isNotNull();
            String sql = result.toString();
            assertThat(sql).contains("OR");
            assertThat(sql).contains("dept_id = " + deptId);
            assertThat(sql).contains("create_by = " + userId);
        }

        @Test
        @DisplayName("多角色 - 多个自定义部门权限合并")
        void whenMultipleCustomRoles_thenMergeWithOr() {
            // given: 用户有两个自定义部门权限的角色
            setSecurityContext(100L, "user", 10L,
                    Set.of(new SimpleGrantedAuthority("ROLE_A"), new SimpleGrantedAuthority("ROLE_B")),
                    List.of(
                            RoleDataScope.custom("A", Arrays.asList(10L, 20L)),
                            RoleDataScope.custom("B", Arrays.asList(30L, 40L))
                    ));

            Expression where = null;

            // when
            Expression result = handler.getSqlSegment(where,
                    "com.youlai.boot.plugin.mybatis.TestDataPermissionMapper.selectList");

            // then: 生成OR连接的IN条件
            assertThat(result).isNotNull();
            String sql = result.toString();
            assertThat(sql).contains("OR");
            assertThat(sql).contains("dept_id IN");
        }

        @Test
        @DisplayName("已有where条件 - 新条件用AND连接")
        void whenExistingWhere_thenAndWithNewCondition() {
            // given
            setSecurityContext(100L, "user", 10L,
                    Set.of(new SimpleGrantedAuthority("ROLE_USER")),
                    List.of(RoleDataScope.dept("USER")));

            Expression where = new EqualsTo(new Column("status"), new net.sf.jsqlparser.expression.LongValue(1));

            // when
            Expression result = handler.getSqlSegment(where,
                    "com.youlai.boot.plugin.mybatis.TestDataPermissionMapper.selectList");

            // then: 结果应该包含原始where和数据权限条件
            assertThat(result).isInstanceOf(AndExpression.class);
            String sql = result.toString();
            assertThat(sql).contains("status = 1");
            assertThat(sql).contains("dept_id = 10");
        }
    }

    // ==================== 注解配置测试 ====================

    @Nested
    @DisplayName("注解配置测试")
    class AnnotationTests {

        @Test
        @DisplayName("无@DataPermission注解 - 返回原始where")
        void whenNoAnnotation_thenReturnOriginalWhere() {
            // given
            setSecurityContext(100L, "user", 10L,
                    Set.of(new SimpleGrantedAuthority("ROLE_USER")),
                    List.of(RoleDataScope.dept("USER")));

            Expression where = new EqualsTo(new Column("status"), new net.sf.jsqlparser.expression.LongValue(1));

            // when: 调用无注解的mapper方法
            Expression result = handler.getSqlSegment(where,
                    "java.lang.Object.toString");

            // then: 返回原始where
            assertThat(result).isSameAs(where);
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 设置安全上下文
     *
     * @param userId     用户ID
     * @param username   用户名
     * @param deptId     部门ID
     * @param authorities 权限集合
     * @param dataScopes 数据权限列表
     */
    private void setSecurityContext(Long userId, String username, Long deptId,
                                    Set<SimpleGrantedAuthority> authorities,
                                    List<RoleDataScope> dataScopes) {
        SysUserDetails userDetails = new SysUserDetails();
        userDetails.setUserId(userId);
        userDetails.setUsername(username);
        userDetails.setDeptId(deptId);
        userDetails.setDataScopes(dataScopes);
        userDetails.setAuthorities(authorities);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, authorities);

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
