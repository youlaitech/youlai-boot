# Security 框架重构与 Starter 抽离方案

> **状态**：待实施
> **范围**：`framework/security` 模块
> **目标**：消除模型冗余 → 端口/适配器解耦 → 抽离为独立 Spring Boot Starter
> **适用**：youlai-boot（单模块）和 youlai-boot-multi（多模块）
> **最后更新**：2026-07-04
> **合并自**：`deep-research-report.md`（Ports & Adapters 研究）+ 原 `security-refactor-plan.md`（模型消除教程）
> **实施策略**：不保留历史登录态；重构发布后所有旧 Token 失效，用户重新登录

---

## 目录

- [〇、文档使用指南（AI 协作必读）](#〇文档使用指南ai-协作必读)
- [一、问题诊断](#一问题诊断)
- [二、架构原则与设计决策](#二架构原则与设计决策)
- [三、方案设计](#三方案设计)
- [四、单模块重构（youlai-boot）](#四单模块重构youlai-boot)
- [五、多模块重构（youlai-boot-multi）](#五多模块重构youlai-boot-multi)
- [六、Security Starter 抽离与发布方案](#六security-starter-抽离与发布方案)
- [七、测试策略](#七测试策略)
- [八、实施阶段](#八实施阶段)
- [九、AI 协作指南](#九ai-协作指南)
- [十、代码审查清单](#十代码审查清单)
- [附录 A：术语表](#附录-a术语表)
- [附录 B：变更日志](#附录-b变更日志)

---

## 〇、文档使用指南（AI 协作必读）

> **本节专为 AI 协作设计。任何 AI 在修改 security 相关代码前，必须先阅读本节。**

### 0.1 文档定位

本文档是 youlai-boot security 模块的**唯一权威重构方案**，合并了早期的架构研究（`deep-research-report.md`）和实施计划。所有 security 相关的架构变更、代码修改、命名约定均以本文档为准。

### 0.2 如何使用本文档

| 你的目标 | 阅读顺序 |
|----------|----------|
| **理解为什么要改** | 第一节 → 第二节 |
| **执行单模块重构** | 第三节 → 第四节 → 第八节（P0→P1→P2→P3）|
| **执行多模块重构** | 第三节 → 第五节 → 第八节（P0→P1→P2→P3） |
| **抽离为 Starter（实操）** | 先完成第四/五节 → 第六节 → 6.11（实操步骤） |
| **发布到 Maven Central** | 6.12（GPG + Sonatype + deploy） |
| **其他项目接入 Starter** | 6.13（引入依赖 + 实现适配器 + 验证） |
| **修改 security 代码** | 第九节（AI 协作指南）→ 第十节（审查清单）|
| **新增端口/适配器** | 第二节 2.3 → 第九节 9.3 |

### 0.3 关键决策摘要（不可违背）

| 编号 | 决策 | 原因 |
|------|------|------|
| D1 | 端口数量固定为 **2 个**：`UserAuthenticationPort`、`PermissionPort` | `RolePort` 冗余——roles 已包含在 `SecurityUser` 中；在线用户不纳入本次重构 |
| D2 | 将 `UserAuthInfo` **重命名为 `SecurityUser`** 作为端口返回模型 | `SecurityUser` 语义更中性，作为 Starter 公开 API 更规范；原 `UserAuthInfo` 偏向"认证信息"但实际承载用户安全数据；重命名而非新建，改动最小 |
| D3 | `SecurityUserDetails.authorities` 字段类型 `Collection<SimpleGrantedAuthority>` → `Set<String> roles` | 消除序列化问题，`getAuthorities()` 改为实时计算 |
| D4 | **删除 `UserSession`**，发布后旧 Token 全部失效 | 其 5 个字段 100% 是 `SecurityUserDetails` 的子集，中间层无意义；不保留历史登录态 |
| D5 | 端口接口放在 `framework.security.port` 包，适配器放在 `system.security.adapter` 包 | 物理隔离端口定义与实现，符合六边形架构 |
| D6 | Starter 模块命名为 **`youlai-security-spring-boot-starter`** | 遵循 Spring Boot 官方 starter 命名规范 |
| D7 | `SocialPlatformEnum` 下沉到 `youlai-common` 后再定义端口 | 端口和 Starter 不允许出现任何 `com.youlai.boot.system.*` 引用 |
| D8 | Starter 不内置业务 JSON 响应写出逻辑，不依赖 `ResponseWriter` | 认证失败和鉴权失败走 Spring Security 标准异常与使用方 `AuthenticationEntryPoint` / `AccessDeniedHandler` |

### 0.4 代码中的空占位文件说明

> ⚠️ **重要发现**：当前 `framework/security/service/` 下已存在 4 个空文件，表明端口模式**已被原开发者规划但未实施**：

| 空文件 | 推测用途 | 重构后处理 |
|--------|----------|------------|
| `UserAuthQueryService.java` | 用户认证查询接口占位 | **删除**，由 `port/UserAuthenticationPort.java` 替代 |
| `RolePermissionService.java` | 角色权限查询接口占位 | **删除**，由 `port/PermissionPort.java` 替代 |
| `WxMaUserAuthQueryService.java` | 微信小程序认证查询占位 | **删除**，合并入 `UserAuthenticationPort.getAuthInfoByOpenid()` |
| `WxMaBindInfo.java` | 微信绑定信息模型占位 | **删除**，微信绑定流程留在使用方 Provider 内实现 |

---

## 一、问题诊断

### 1.1 模型冗余（3 → 2）

当前三个模型字段高度重叠（`SecurityUser` 为 `UserAuthInfo` 重命名后的名称，见 D2）：

| 字段 | `SecurityUser`<br>（原 `UserAuthInfo`） | `SecurityUserDetails` | `UserSession` |
|------|:---:|:---:|:---:|
| userId | ✓ | ✓ | ✓ |
| username | ✓ | ✓ | ✓ |
| deptId | ✓ | ✓ | ✓ |
| dataScopes | ✓ | ✓ | ✓ |
| roles | ✓ | — | ✓ |
| authorities | — | ✓ | — |
| password | ✓ | ✓ | — |
| enabled | — | ✓ | — |
| nickname | ✓ | — | — |
| status | ✓ | — | — |

**数据流**：`SecurityUser → SecurityUserDetails → UserSession → SecurityUserDetails`，中间经历两次无意义字段拷贝。`UserSession` 的 5 个字段 100% 是 `SecurityUserDetails` 的子集，存在的唯一原因是 `SimpleGrantedAuthority` 不便序列化。

### 1.2 框架 ↔ 业务循环依赖

```
framework.security ──→ system（SecurityUserDetailsService 直接注入 UserService、UserSocialService）
framework.security ──→ system（PermissionService 直接注入 RoleMenuService）
system ──→ framework.security（SecurityUtils、SecurityUser、SecurityUserDetails）
```

`SecurityUserDetailsService` 和 `PermissionService` 中大量 `import com.youlai.boot.system.*`，违反了依赖方向。在多模块项目中，这导致 `youlai-framework` 无法独立于 `youlai-system` 发布。

### 1.3 序列化问题（已临时修复）

`JacksonJsonRedisSerializer<Object>` 反序列化时丢失类型信息，`UserSession` 被还原为 `LinkedHashMap`。

**当前修复方案**：提取 `JsonMapper` 为共享 Bean，使用 `convertValue` 显式转换。✅ 已生效，但 `UserSession` 中间层本身是不必要的。

### 1.4 现状 vs 目标对比

| 维度 | 当前实现（耦合严重） | 目标实现（Ports & Adapters） |
|------|---------------------|------------------------------|
| **依赖关系** | `framework/security` 直接 `import com.youlai.boot.system.*` | 安全模块仅引用自身端口接口，不持有 system 引用 |
| **用户认证** | `SecurityUserDetailsService` 注入 `UserService` 查询 | 注入 `UserAuthenticationPort`，由 system 侧适配器实现 |
| **权限校验** | `PermissionService` 注入 `RoleMenuService` | 注入 `PermissionPort`，由 system 侧适配器实现 |
| **角色获取** | `SecurityUserDetails` 构造时转换 `authorities` | `roles` 字段直接存储 `Set<String>`，`getAuthorities()` 实时计算 |
| **在线用户** | 无专门接口 | 不纳入本次 Security Starter 重构 |
| **序列化** | `UserSession` 中间层 + `convertValue` 修补 | 直接存取 `SecurityUserDetails`（纯 JDK 类型） |
| **可测试性** | 安全代码测试需引入 system 服务 | 针对端口接口 Mock 测试，解耦 |
| **模块化** | 无法独立发布 security 模块 | 端口定义可随 Starter 独立发布 |

---

## 二、架构原则与设计决策

### 2.1 Ports & Adapters 模式（精简）

**六边形架构**核心思想：应用核心业务通过抽象的**端口（Port）**与外部通信，外部实现通过**适配器（Adapter）**连入端口。

- **端口（Port）**：定义安全模块需要的功能接口，放在 `framework.security.port` 包。命名以 `Port` 后缀结尾。
- **适配器（Adapter）**：在 `system` 模块实现端口接口，内部调用具体 Service。标注 `@Component`。
- **解耦效果**：安全模块编译期不持有任何 `com.youlai.boot.system.*` 引用。

### 2.2 端口定义决策

| 端口 | 方法 | 职责 | 是否核心 |
|------|------|------|:---:|
| `UserAuthenticationPort` | `getAuthInfoByUsername(String)`<br>`getAuthInfoByMobile(String)`<br>`getAuthInfoByOpenid(SocialPlatformEnum, String)` | 用户认证信息查询 | ✅ |
| `PermissionPort` | `getRolePerms(Set<String> roleCodes)` | 角色权限集合查询 | ✅ |

> **为什么不设 `RolePort`？**
> 角色编码（roles）已作为 `Set<String>` 包含在 `SecurityUser` 中，登录时一次性获取，无需单独端口查询。`SecurityUtils.getRoles()` 直接从 `SecurityUserDetails.roles` 字段取值即可。

> **为什么不做在线用户端口？**
> 在线用户管理属于业务运营能力，不属于认证鉴权最小内核。本次重构不提供 `OnlineUserPort`，后续如需在线用户功能，由业务模块基于登录/登出事件自行实现，不进入 Security Starter。

### 2.3 命名规范

| 类型 | 命名规则 | 示例 |
|------|----------|------|
| 端口接口 | `<领域意图>Port` | `UserAuthenticationPort`、`PermissionPort` |
| 适配器实现 | `<领域意图>Adapter` | `UserAuthenticationAdapter`、`PermissionAdapter` |
| 端口包路径 | `framework.security.port` | — |
| 适配器包路径 | `system.security.adapter`（单模块）<br>`youlai-system/.../security/adapter`（多模块） | — |
| 端口返回模型 | `SecurityUser`（由 `UserAuthInfo` 重命名，纯 POJO，无 system 依赖） | — |

> **禁止**：使用 `Service`/`Query` 后缀命名端口（如 `UserAuthQueryService`），这与业务 Service 混淆。当前代码中的空文件 `UserAuthQueryService.java`、`RolePermissionService.java` 就是错误命名的占位，应删除并用 `Port` 后缀替代。

---

## 三、方案设计

### 3.1 消除 UserSession

**核心改动**：`SecurityUserDetails` 将 `Collection<SimpleGrantedAuthority> authorities` 替换为 `Set<String> roles`，`getAuthorities()` 改为实时计算。

```java
@Data
@NoArgsConstructor
public class SecurityUserDetails implements UserDetails {

    private Long userId;
    private String username;
    private String password;
    private Boolean enabled;
    private Long deptId;
    private List<RoleDataScope> dataScopes;
    private Set<String> roles;  // ← 替换 authorities

    public SecurityUserDetails(SecurityUser user) {
        this.userId = user.getUserId();
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.enabled = ObjectUtil.equal(user.getStatus(), 1);
        this.deptId = user.getDeptId();
        this.dataScopes = user.getDataScopes();
        this.roles = user.getRoles();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (CollectionUtil.isEmpty(roles)) {
            return Collections.emptySet();
        }
        return roles.stream()
            .map(role -> new SimpleGrantedAuthority(SecurityConstants.ROLE_PREFIX + role))
            .collect(Collectors.toSet());
    }

    // 其他 UserDetails 方法不变...
}
```

**效果**：所有字段均为纯 JDK 类型，`SecurityUserDetails` 可直接序列化存储，`UserSession` 文件删除。

### 3.2 端口/适配器解耦

```
┌─────────────────────────────────┐     ┌──────────────────────────────────┐
│      framework/security         │     │           system                 │
│  ┌───────────────────────────┐  │     │  ┌────────────────────────────┐  │
│  │  port/                    │  │     │  │  security/adapter/         │  │
│  │  ├ UserAuthenticationPort │◄─┼─────┼──│  ├ UserAuthenticationAdapter│  │
│  │  └ PermissionPort         │◄─┼─────┼──│  └ PermissionAdapter        │  │
│  └───────────────────────────┘  │     │  └────────────────────────────┘  │
│  ┌───────────────────────────┐  │     │  ┌────────────────────────────┐  │
│  │  service/                 │  │     │  │  service/                  │  │
│  │  ├ SecurityUserDetailsService  │──┼─────┼──│  （注入 Port，不注入 system）│  │
│  │  └ PermissionService      │──┼─────┼──│                             │  │
│  └───────────────────────────┘  │     │  └────────────────────────────┘  │
└─────────────────────────────────┘     └──────────────────────────────────┘
         ↑ 编译期零 system 依赖                     ↑ 运行期注入 Port 实现
```

**端口定义**（framework 层）：

| 端口 | 方法 | 职责 |
|------|------|------|
| `UserAuthenticationPort` | `getAuthInfoByUsername(String)`<br>`getAuthInfoByMobile(String)`<br>`getAuthInfoByOpenid(platform, openid)` | 用户认证信息查询 |
| `PermissionPort` | `getRolePerms(Set<String> roleCodes)` | 角色权限集合查询 |

**适配器实现**（system 层）：

| 适配器 | 实现端口 | 委托目标 |
|--------|----------|----------|
| `UserAuthenticationAdapter` | `UserAuthenticationPort` | `UserService` + `UserSocialService` |
| `PermissionAdapter` | `PermissionPort` | `RoleMenuService` |

### 3.3 SecurityUtils 简化

```java
// 之前：5 步链式（getAuthorities → filter ROLE_ → strip）
// 之后：直接取 roles 字段
public static Set<String> getRoles() {
    return getUser().map(SecurityUserDetails::getRoles).orElse(Set.of());
}
```

### 3.4 Token 管理器适配

- **RedisTokenManager**：直接存储 `SecurityUserDetails`（password 置 null），删除 `UserSession` 中间层和 `buildUserDetails` 重建逻辑
- **JwtTokenManager**：JWT claims 使用 `roles` 字段保存不带 `ROLE_` 前缀的角色编码；`parseToken` 只解析 `roles`，`getAuthorities()` 实时补 `ROLE_` 前缀
- **旧 Token 策略**：不接受历史 JWT / Redis Token。发布时清理 Redis 中 `auth:*` 登录态 key，并轮换 `security.session.jwt.secret-key`，所有用户重新登录

---

## 四、单模块重构（youlai-boot）

### 4.1 端口接口定义

先将 `SocialPlatformEnum` 下沉到 `youlai-common`，再新建包 `framework.security.port`：

```
framework/security/
├── port/
│   ├── UserAuthenticationPort.java      # 新增
│   └── PermissionPort.java              # 新增
├── model/
│   ├── SecurityUserDetails.java              # 改造
│   ├── SecurityUser.java                # 重命名（原 UserAuthInfo）
│   └── UserSession.java                 # 删除
├── service/
│   ├── SecurityUserDetailsService.java       # 改造（注入 Port）
│   ├── PermissionService.java           # 改造（注入 Port）
│   ├── UserAuthQueryService.java        # 删除（空占位）
│   ├── RolePermissionService.java       # 删除（空占位）
│   ├── WxMaUserAuthQueryService.java    # 删除（空占位）
│   └── WxMaBindInfo.java                # 删除（空占位，如在 model 包则一并清理）
└── ...
```

```java
// framework/security/port/UserAuthenticationPort.java
package com.youlai.boot.framework.security.port;

import com.youlai.boot.framework.security.model.SecurityUser;
import com.youlai.boot.common.enums.SocialPlatformEnum;

/**
 * 用户认证信息查询端口。
 * <p>
 * 由 system 模块提供适配器实现，framework 层通过此接口获取认证数据。
 * 框架层不直接依赖 system 模块的 Service。
 *
 * @see com.youlai.boot.system.security.adapter.UserAuthenticationAdapter
 */
public interface UserAuthenticationPort {

    /**
     * 根据用户名查询认证信息。
     *
     * @param username 用户名
     * @return 认证信息，不存在返回 null
     */
    SecurityUser getAuthInfoByUsername(String username);

    /**
     * 根据手机号查询认证信息。
     *
     * @param mobile 手机号
     * @return 认证信息，不存在返回 null
     */
    SecurityUser getAuthInfoByMobile(String mobile);

    /**
     * 根据第三方 openid 查询认证信息。
     *
     * @param platform 第三方平台
     * @param openid   openid
     * @return 认证信息，未绑定返回 null
     */
    SecurityUser getAuthInfoByOpenid(SocialPlatformEnum platform, String openid);
}
```

```java
// framework/security/port/PermissionPort.java
package com.youlai.boot.framework.security.port;

import java.util.Set;

/**
 * 权限查询端口。
 * <p>
 * 由 system 模块提供适配器实现，framework 层通过此接口获取角色权限集合。
 *
 * @see com.youlai.boot.system.security.adapter.PermissionAdapter
 */
public interface PermissionPort {

    /**
     * 根据角色编码集合查询权限标识集合。
     *
     * @param roleCodes 角色编码集合
     * @return 权限标识集合，如 "sys:user:create"
     */
    Set<String> getRolePerms(Set<String> roleCodes);
}
```

> **硬性要求**：`UserAuthenticationPort` 必须引用 `com.youlai.boot.common.enums.SocialPlatformEnum`。端口接口中禁止出现任何 `com.youlai.boot.system.*` 引用。

### 4.2 SecurityUserDetails 改造

```java
// framework/security/model/SecurityUserDetails.java
// 改动点：
// 1. authorities 字段类型 Collection<SimpleGrantedAuthority> → Set<String> roles
// 2. 构造函数直接赋值 roles
// 3. getAuthorities() 改为实时计算

@Data
@NoArgsConstructor
public class SecurityUserDetails implements UserDetails {

    private Long userId;
    private String username;
    private String password;
    private Boolean enabled;
    private Long deptId;
    private List<RoleDataScope> dataScopes;
    private Set<String> roles;  // ← 替换 authorities

    public SecurityUserDetails(SecurityUser user) {
        this.userId = user.getUserId();
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.enabled = ObjectUtil.equal(user.getStatus(), 1);
        this.deptId = user.getDeptId();
        this.dataScopes = user.getDataScopes();
        this.roles = user.getRoles();  // ← 直接赋值，无需转换
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (CollectionUtil.isEmpty(roles)) {
            return Collections.emptySet();
        }
        return roles.stream()
            .map(role -> new SimpleGrantedAuthority(SecurityConstants.ROLE_PREFIX + role))
            .collect(Collectors.toSet());
    }
}
```

### 4.3 适配器实现

在 system 模块新建 `system.security.adapter` 包：

```java
// system/security/adapter/UserAuthenticationAdapter.java
package com.youlai.boot.system.security.adapter;

import com.youlai.boot.framework.security.model.SecurityUser;
import com.youlai.boot.framework.security.port.UserAuthenticationPort;
import com.youlai.boot.common.enums.SocialPlatformEnum;
import com.youlai.boot.system.service.UserService;
import com.youlai.boot.system.service.UserSocialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 用户认证信息查询适配器。
 * <p>
 * 实现 framework 层的 UserAuthenticationPort，委托 system 层服务完成查询。
 * 此类是端口与系统服务的唯一耦合点。
 */
@Component
@RequiredArgsConstructor
public class UserAuthenticationAdapter implements UserAuthenticationPort {

    private final UserService userService;
    private final UserSocialService userSocialService;

    @Override
    public SecurityUser getAuthInfoByUsername(String username) {
        return userService.getAuthInfoByUsername(username);
    }

    @Override
    public SecurityUser getAuthInfoByMobile(String mobile) {
        return userService.getAuthInfoByMobile(mobile);
    }

    @Override
    public SecurityUser getAuthInfoByOpenid(SocialPlatformEnum platform, String openid) {
        return userSocialService.getAuthInfoByOpenid(platform, openid);
    }
}
```

```java
// system/security/adapter/PermissionAdapter.java
package com.youlai.boot.system.security.adapter;

import com.youlai.boot.framework.security.port.PermissionPort;
import com.youlai.boot.system.service.RoleMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Set;

/**
 * 权限查询适配器。
 * <p>
 * 实现 framework 层的 PermissionPort，委托 RoleMenuService 查询权限。
 */
@Component
@RequiredArgsConstructor
public class PermissionAdapter implements PermissionPort {

    private final RoleMenuService roleMenuService;

    @Override
    public Set<String> getRolePerms(Set<String> roleCodes) {
        return roleMenuService.getRolePermsByRoleCodes(roleCodes);
    }
}
```

### 4.4 服务层改造

```java
// framework/security/service/SecurityUserDetailsService.java（改造后）
package com.youlai.boot.framework.security.service;

import com.youlai.boot.framework.security.model.SecurityUserDetails;
import com.youlai.boot.framework.security.model.SecurityUser;
import com.youlai.boot.framework.security.port.UserAuthenticationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 系统用户认证 DetailsService。
 * <p>
 * 通过 UserAuthenticationPort 获取认证信息，不直接依赖 system 模块。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityUserDetailsService implements UserDetailsService {

    private final UserAuthenticationPort userAuthPort;  // ← 替换 UserService

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            SecurityUser SecurityUser = userAuthPort.getAuthInfoByUsername(username);
            if (SecurityUser == null) {
                throw new UsernameNotFoundException(username);
            }
            return new SecurityUserDetails(SecurityUser);
        } catch (Exception e) {
            log.error("认证异常:{}", e.getMessage());
            throw e;
        }
    }
}
```

```java
// framework/security/service/PermissionService.java（改造后）
package com.youlai.boot.framework.security.service;

import com.youlai.boot.framework.security.port.PermissionPort;
import com.youlai.boot.framework.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.PatternMatchUtils;
import java.util.Set;

@Component("ss")
@RequiredArgsConstructor
@Slf4j
public class PermissionService {

    private final PermissionPort permissionPort;  // ← 替换 RoleMenuService

    public boolean hasPerm(String requiredPerm) {
        // ... 前置校验不变（超管放行等）...
        Set<String> roleCodes = SecurityUtils.getRoles();
        Set<String> rolePerms = permissionPort.getRolePerms(roleCodes);  // ← 通过 Port 调用
        // ... 后续逻辑不变（通配符匹配等）...
    }
}
```

### 4.5 Token 管理器适配

**RedisTokenManager** 改动：

```java
// 之前：UserSession session = new UserSession(user);
//       redisTemplate.opsForValue().set(key, session);
// 之后：直接存 SecurityUserDetails（password 置 null）
SecurityUserDetails sessionUser = new SecurityUserDetails();
sessionUser.setUserId(user.getUserId());
sessionUser.setUsername(user.getUsername());
sessionUser.setDeptId(user.getDeptId());
sessionUser.setDataScopes(user.getDataScopes());
sessionUser.setRoles(user.getRoles());
sessionUser.setEnabled(user.isEnabled());
sessionUser.setPassword(null);  // 不存密码
redisTemplate.opsForValue().set(key, sessionUser);
```

```java
// 之前：UserSession raw = jsonMapper.convertValue(obj, UserSession.class);
//       return buildUserDetails(raw);
// 之后：直接还原
SecurityUserDetails raw = jsonMapper.convertValue(obj, SecurityUserDetails.class);
return raw;
```

**JwtTokenManager** 改动：

```java
// parseToken 中：
// 之前：从 claims 取 authorities，构造 Set<SimpleGrantedAuthority>
// 之后：从 claims 取 roles（Set<String>），直接 setRoles()
Set<String> roles = claims.get(JwtClaimConstants.ROLES);
securityUserDetails.setRoles(roles);
// getAuthorities() 会在运行时自动 add ROLE_ 前缀
```

同步修改 `JwtClaimConstants`：

```java
// 删除 AUTHORITIES，新增 ROLES
String ROLES = "roles";
```

上线发布时执行登录态失效：

```text
1. 清理 Redis 中 auth:* 登录态 key
2. 轮换 security.session.jwt.secret-key
3. 通知前端收到 401 后跳转登录页
```

### 4.6 单模块改动清单

| 文件 | 操作 | 改动 |
|------|------|------|
| `framework/security/port/UserAuthenticationPort.java` | **新增** | 端口接口 |
| `framework/security/port/PermissionPort.java` | **新增** | 端口接口 |
| `framework/security/model/SecurityUserDetails.java` | 改造 | `authorities` → `roles`，`getAuthorities()` 改为计算 |
| `framework/security/model/UserSession.java` | **删除** | 不再需要 |
| `framework/security/service/SecurityUserDetailsService.java` | 改造 | 注入 `UserAuthenticationPort` 替代 `UserService` |
| `framework/security/service/PermissionService.java` | 改造 | 注入 `PermissionPort` 替代 `RoleMenuService` |
| `framework/security/config/SecurityConfig.java` | 改造 | 移除 `UserService` 直接注入，Provider 改走端口或留在使用方 |
| `framework/security/service/UserAuthQueryService.java` | **删除** | 空占位文件 |
| `framework/security/service/RolePermissionService.java` | **删除** | 空占位文件 |
| `framework/security/service/WxMaUserAuthQueryService.java` | **删除** | 空占位文件 |
| `framework/security/model/WxMaBindInfo.java` | **删除** | 空占位文件 |
| `framework/security/util/SecurityUtils.java` | 改造 | `getRoles()` 简化为直接取 `roles` 字段 |
| `framework/security/token/RedisTokenManager.java` | 改造 | 存 `SecurityUserDetails`，删除 `UserSession` 引用和 `buildUserDetails` |
| `framework/security/token/JwtTokenManager.java` | 改造 | `parseToken` 设 `roles` 替代 `authorities` |
| `common/constant/JwtClaimConstants.java` | 改造 | 删除 `AUTHORITIES`，新增 `ROLES = "roles"` |
| `common/enums/SocialPlatformEnum.java` | **新增/迁移** | 从 `system.enums` 下沉到 common |
| `system/security/adapter/UserAuthenticationAdapter.java` | **新增** | 适配器实现 |
| `system/security/adapter/PermissionAdapter.java` | **新增** | 适配器实现 |

---

## 五、多模块重构（youlai-boot-multi）

多模块项目中，`youlai-framework` 是独立 Maven 模块，`youlai-system` 是另一个模块。核心区别：端口定义在 `youlai-framework`，适配器实现在 `youlai-system`。

### 5.1 模块依赖关系

```
youlai-framework（端口定义 + 基础设施）  ←──依赖── youlai-system（适配器实现 + 业务）
       ↑                                              ↑
       └──依赖── youlai-auth、youlai-application 等
```

> **关键**：`youlai-framework` 的 `pom.xml` **不依赖** `youlai-system`。`youlai-system` 依赖 `youlai-framework`，实现端口接口。

### 5.2 当前多模块状态

多模块项目已将 `SecurityUserDetailsService` 和 `PermissionService` 移至 `youlai-system/security/service/`，但代码仍直接 `import` system 服务，循环依赖未解决。`youlai-framework/security/service/` 目录为空。

> ⚠️ **版本不一致提醒**：根 POM 声明 `4.3.1`，而 `youlai-framework` 和 `youlai-system` 的 parent 版本为 `4.3.0`。重构时应一并修正。

### 5.3 端口接口定义（youlai-framework）

在 `youlai-framework` 模块新建 `port` 包：

```
youlai-framework/src/main/java/com/youlai/boot/framework/security/
├── port/
│   ├── UserAuthenticationPort.java      # 新增
│   └── PermissionPort.java              # 新增
├── model/
│   ├── SecurityUserDetails.java              # 改造（同单模块 4.2）
│   ├── SecurityUser.java                # 重命名（原 UserAuthInfo）
│   └── UserSession.java                 # 删除
├── token/
│   ├── TokenManager.java                # 不变
│   ├── JwtTokenManager.java             # 改造（同单模块 4.5）
│   └── RedisTokenManager.java           # 改造（同单模块 4.5）
├── util/
│   └── SecurityUtils.java               # 改造（同单模块 3.3）
└── service/                             # 保持为空（服务在 system 模块）
```

> 端口接口代码与单模块完全相同（见 4.1），仅包名一致。

### 5.4 适配器实现（youlai-system）

在 `youlai-system` 模块新建 `adapter` 包（当前**不存在** adapter 目录，需创建）：

```
youlai-system/src/main/java/com/youlai/boot/system/security/
├── adapter/                               # ← 新建目录
│   ├── UserAuthenticationAdapter.java     # 新增
│   └── PermissionAdapter.java             # 新增
├── service/
│   ├── SecurityUserDetailsService.java         # 改造（注入 Port）
│   └── PermissionService.java             # 改造（注入 Port）
├── provider/
│   ├── SmsAuthenticationProvider.java     # 使用方业务 Provider，留在 system
│   └── WxMaAuthenticationProvider.java    # 使用方业务 Provider，留在 system
└── config/
    └── SecurityConfig.java                # 检查注入对象
```

> 适配器代码与单模块完全相同（见 4.3），仅位于 `youlai-system` 模块。

### 5.5 服务层改造（youlai-system）

`SecurityUserDetailsService` 和 `PermissionService` 已在 `youlai-system` 模块中，改造方式与单模块相同（见 4.4），注入端口接口替代直接服务引用。

`SmsAuthenticationProvider` 和 `WxMaAuthenticationProvider` 属于使用方业务认证流程，留在 `youlai-system`。它们可以直接调用 `UserService` / `UserSocialService` 完成验证码、微信绑定、session_key 更新等业务动作，但不得迁入 Starter。

### 5.6 POM 依赖检查

```xml
<!-- youlai-framework/pom.xml：不依赖 youlai-system -->
<dependencies>
    <dependency>
        <groupId>com.youlai</groupId>
        <artifactId>youlai-common</artifactId>
    </dependency>
    <!-- 不包含 youlai-system -->
</dependencies>

<!-- youlai-system/pom.xml：依赖 youlai-framework -->
<dependencies>
    <dependency>
        <groupId>com.youlai</groupId>
        <artifactId>youlai-framework</artifactId>
    </dependency>
</dependencies>
```

### 5.7 多模块改动清单

| 模块 | 文件 | 操作 |
|------|------|------|
| `youlai-framework` | `security/port/UserAuthenticationPort.java` | 新增 |
| `youlai-framework` | `security/port/PermissionPort.java` | 新增 |
| `youlai-framework` | `security/model/SecurityUserDetails.java` | 改造 |
| `youlai-framework` | `security/model/UserSession.java` | **删除** |
| `youlai-framework` | `security/util/SecurityUtils.java` | 改造 |
| `youlai-framework` | `security/token/JwtTokenManager.java` | 改造 |
| `youlai-framework` | `security/token/RedisTokenManager.java` | 改造 |
| `youlai-framework` | `pom.xml` | 确认不含 youlai-system 依赖 |
| `youlai-system` | `security/adapter/UserAuthenticationAdapter.java` | 新增 |
| `youlai-system` | `security/adapter/PermissionAdapter.java` | 新增 |
| `youlai-system` | `security/service/SecurityUserDetailsService.java` | 改造 |
| `youlai-system` | `security/service/PermissionService.java` | 改造 |
| `youlai-system` | `security/provider/SmsAuthenticationProvider.java` | 保留在使用方，不迁入 Starter |
| `youlai-system` | `security/provider/WxMaAuthenticationProvider.java` | 保留在使用方，不迁入 Starter |
| `youlai-system` | `pom.xml` | parent 版本修正为 4.3.1 |

---

## 六、Security Starter 抽离与发布方案

> **前提**：完成第四节（单模块）或第五节（多模块）的端口/适配器解耦后，方可执行本节。

### 6.1 可行性分析

| 维度 | 当前状态 | Starter 要求 | 差距 |
|------|----------|--------------|------|
| 端口定义 | 散落在 `framework.security` | 需独立模块 | 需新建 Maven 模块 |
| 自动装配 | 无 `spring.factories` / `AutoConfiguration.imports` | 需 SPI 注册 | 需新建自动配置类 |
| 配置属性 | `SecurityProperties` 已有 `@ConfigurationProperties` | 需 `@EnableConfigurationProperties` | 改造 |
| Token 实现 | 固定保留 `JwtTokenManager` 与 `RedisTokenManager` 两个实现 | 按 `security.session.type` 条件装配 | 改造 |
| 业务耦合 | `SocialPlatformEnum` 引用 system 包 | 需下沉到 common | 改造 |
| 依赖管理 | security 与 cache/mybatis/captcha 混在 framework | 需独立依赖树 | 新建 pom.xml |

**结论**：可行。端口/适配器解耦是前置条件，解耦后 security 模块编译期零 system 依赖，符合 Starter 抽离要求。

### 6.2 Starter 模块设计

```
youlai-boot-multi/（多模块项目根）
├── youlai-common/                    # 常量、枚举（SocialPlatformEnum 下沉至此）
├── youlai-security-spring-boot-starter/   # ← 新增：Security Starter
│   ├── src/main/java/com/youlai/boot/framework/security/
│   │   ├── autoconfigure/            # 自动配置类
│   │   │   ├── SecurityAutoConfiguration.java
│   │   │   ├── JwtTokenAutoConfiguration.java
│   │   │   └── RedisTokenAutoConfiguration.java
│   │   ├── port/                     # 端口接口（对外契约）
│   │   │   ├── UserAuthenticationPort.java
│   │   │   └── PermissionPort.java
│   │   ├── model/                    # 安全模型
│   │   │   ├── SecurityUserDetails.java
│   │   │   ├── SecurityUser.java
│   │   │   ├── RoleDataScope.java
│   │   │   └── ...（Token 模型等）
│   │   ├── service/                  # 框架层服务
│   │   │   ├── SecurityUserDetailsService.java
│   │   │   └── PermissionService.java
│   │   ├── token/                    # Token 管理器
│   │   │   ├── TokenManager.java
│   │   │   ├── JwtTokenManager.java
│   │   │   └── RedisTokenManager.java
│   │   ├── filter/                   # Token 认证过滤器
│   │   │   └── TokenAuthenticationFilter.java
│   │   ├── exception/                # 异常
│   │   ├── util/                     # SecurityUtils
│   │   └── config/                   # SecurityProperties
│   ├── src/main/resources/
│   │   └── META-INF/
│   │       └── spring/
│   │           └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
│   └── pom.xml
├── youlai-system/                    # 提供 Adapter 实现
├── youlai-framework/                 # 剥离 security 后，保留 cache/mybatis 等
└── youlai-application/               # 引用 starter
```

### 6.3 pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.youlai</groupId>
        <artifactId>youlai-boot</artifactId>
        <version>4.3.1</version>
    </parent>

    <artifactId>youlai-security-spring-boot-starter</artifactId>
    <description>youlai Security Spring Boot Starter - 认证鉴权自动装配</description>

    <dependencies>
        <!-- 内部依赖 -->
        <dependency>
            <groupId>com.youlai</groupId>
            <artifactId>youlai-common</artifactId>
        </dependency>

        <!-- Spring Security -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Redis：JWT tokenVersion/黑名单与 Redis-Token 均依赖 RedisTemplate，Starter 固定携带 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>

        <!-- JWT：沿用项目当前 Hutool JWT 实现，不引入 jjwt 双体系 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
        </dependency>

        <!-- 自动配置元数据（IDE 提示用） -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-autoconfigure</artifactId>
        </dependency>
    </dependencies>
</project>
```

> **关键设计**：Starter 固定携带 Security、Web、Redis、Hutool JWT 所需依赖。使用方不再拼装 JWT/Redis 依赖，降低接入复杂度。

### 6.4 自动配置类

```java
// autoconfigure/SecurityAutoConfiguration.java
package com.youlai.boot.framework.security.autoconfigure;

import com.youlai.boot.framework.security.config.SecurityProperties;
import com.youlai.boot.framework.security.filter.TokenAuthenticationFilter;
import com.youlai.boot.framework.security.port.PermissionPort;
import com.youlai.boot.framework.security.port.UserAuthenticationPort;
import com.youlai.boot.framework.security.service.PermissionService;
import com.youlai.boot.framework.security.service.SecurityUserDetailsService;
import com.youlai.boot.framework.security.token.TokenManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetailsService;
import tools.jackson.databind.json.JsonMapper;

/**
 * Security 自动配置入口。
 * <p>
 * 装配条件：classpath 存在 SecurityProperties 类。
 * 使用方可通过 spring.autoconfigure.exclude 排除整体自动配置。
 */
@AutoConfiguration
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityAutoConfiguration {

    /**
     * 密码编码器，默认 BCrypt。
     */
    @Bean
    @ConditionalOnMissingBean(PasswordEncoder.class)
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * JSON 映射器，供 RedisTokenManager 反序列化使用。
     */
    @Bean
    @ConditionalOnMissingBean(JsonMapper.class)
    public JsonMapper jsonMapper() {
        return JsonMapper.builder().findAndAddModules().build();
    }

    /**
     * 用户认证服务。
     * <p>
     * 需要使用方提供 UserAuthenticationPort 实现。
     */
    @Bean
    @ConditionalOnMissingBean(UserDetailsService.class)
    public UserDetailsService userDetailsService(UserAuthenticationPort userAuthenticationPort) {
        return new SecurityUserDetailsService(userAuthenticationPort);
    }

    /**
     * 权限服务 Bean（SpEL @ss.hasPerm(...) 使用）。
     * <p>
     * 需要 PermissionPort 实现，否则启动失败——
     * 使用方必须提供 PermissionAdapter。
     */
    @Bean("ss")
    @ConditionalOnMissingBean(name = "ss")
    @ConditionalOnProperty(prefix = "security", name = "enabled", havingValue = "true", matchIfMissing = true)
    public PermissionService permissionService(PermissionPort permissionPort) {
        return new PermissionService(permissionPort);
    }

    /**
     * Token 认证过滤器。
     * <p>
     * 过滤器不写业务 JSON 响应；无效 Token 抛出 Spring Security AuthenticationException，
     * 由使用方 SecurityConfig 中的 AuthenticationEntryPoint 统一处理。
     */
    @Bean
    @ConditionalOnMissingBean(TokenAuthenticationFilter.class)
    public TokenAuthenticationFilter tokenAuthenticationFilter(TokenManager tokenManager) {
        return new TokenAuthenticationFilter(tokenManager);
    }
}
```

```java
// autoconfigure/JwtTokenAutoConfiguration.java
package com.youlai.boot.framework.security.autoconfigure;

import com.youlai.boot.framework.security.token.JwtTokenManager;
import com.youlai.boot.framework.security.token.TokenManager;
import com.youlai.boot.framework.security.config.SecurityProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * JWT Token 管理器自动配置。
 * <p>
 * 装配条件：
 * 1. classpath 存在 cn.hutool.jwt.JWTUtil
 * 2. security.session.type = jwt
 */
@AutoConfiguration
@ConditionalOnClass(name = "cn.hutool.jwt.JWTUtil")
@ConditionalOnProperty(prefix = "security.session", name = "type", havingValue = "jwt", matchIfMissing = true)
public class JwtTokenAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(TokenManager.class)
    public TokenManager jwtTokenManager(SecurityProperties properties,
                                        RedisTemplate<String, Object> redisTemplate) {
        return new JwtTokenManager(properties, redisTemplate);
    }
}
```

```java
// autoconfigure/RedisTokenAutoConfiguration.java
package com.youlai.boot.framework.security.autoconfigure;

import com.youlai.boot.framework.security.token.RedisTokenManager;
import com.youlai.boot.framework.security.token.TokenManager;
import com.youlai.boot.framework.security.config.SecurityProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.json.JsonMapper;

/**
 * Redis Token 管理器自动配置。
 * <p>
 * 装配条件：
 * 1. classpath 存在 RedisTemplate
 * 2. security.session.type = redis-token
 */
@AutoConfiguration
@ConditionalOnClass(RedisTemplate.class)
@ConditionalOnProperty(prefix = "security.session", name = "type", havingValue = "redis-token")
public class RedisTokenAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(TokenManager.class)
    public TokenManager redisTokenManager(SecurityProperties properties,
                                          RedisTemplate<String, Object> redisTemplate,
                                          JsonMapper jsonMapper) {
        return new RedisTokenManager(properties, redisTemplate, jsonMapper);
    }
}
```

### 6.5 SPI 注册文件

```
src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

```
com.youlai.boot.framework.security.autoconfigure.SecurityAutoConfiguration
com.youlai.boot.framework.security.autoconfigure.JwtTokenAutoConfiguration
com.youlai.boot.framework.security.autoconfigure.RedisTokenAutoConfiguration
```

> **Spring Boot 3.x+** 使用 `AutoConfiguration.imports`（非 `spring.factories`）。本项目 Spring Boot 4.0，必须使用此方式。

### 6.6 配置属性

```yaml
# application.yml（使用方配置示例）
security:
  enabled: true
  session:
    type: jwt                      # jwt | redis-token
    access-token-time-to-live: 7200
    refresh-token-time-to-live: 604800
    jwt:
      secret-key: ${JWT_SECRET:replace-with-strong-secret}
    redis-token:
      allow-multi-login: true
  ignore-urls:
    - /api/v1/auth/login/**
    - /api/v1/auth/refresh-token
  unsecured-urls:
    - /doc.html
    - /swagger-ui/**
    - /v3/api-docs/**
```

```java
// config/SecurityProperties.java（Starter 内）
@Data
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {
    private boolean enabled = true;
    private SessionConfig session = new SessionConfig();
    private String[] ignoreUrls = new String[0];
    private String[] unsecuredUrls = new String[0];

    @Data
    public static class SessionConfig {
        private String type = "jwt";
        private Integer accessTokenTimeToLive = 7200;
        private Integer refreshTokenTimeToLive = 604800;
        private JwtConfig jwt = new JwtConfig();
        private RedisTokenConfig redisToken = new RedisTokenConfig();
    }

    @Data
    public static class JwtConfig {
        private String secretKey;
    }

    @Data
    public static class RedisTokenConfig {
        private Boolean allowMultiLogin = true;
    }
}
```

### 6.7 枚举下沉处理

`UserAuthenticationPort.getAuthInfoByOpenid(SocialPlatformEnum, String)` 必须引用公共枚举。定义端口前先处理：

**方案**：将 `SocialPlatformEnum` 从 `youlai-system` 下沉到 `youlai-common`：

```
# 之前
youlai-system/src/main/java/com/youlai/boot/system/enums/SocialPlatformEnum.java

# 之后
youlai-common/src/main/java/com/youlai/boot/common/enums/SocialPlatformEnum.java
```

同步修改所有引用方的 import 路径。Starter 的 `UserAuthenticationPort` 只引用 `com.youlai.boot.common.enums.SocialPlatformEnum`。

### 6.8 接入契约摘要

**使用方 pom.xml**：

```xml
<dependency>
    <groupId>com.youlai</groupId>
    <artifactId>youlai-security-spring-boot-starter</artifactId>
    <version>4.3.1</version>
</dependency>
```

**使用方实现 Adapter**（必须）：

```java
// 使用方的 system 模块必须提供两个适配器
@Component
public class MyUserAuthenticationAdapter implements UserAuthenticationPort { ... }

@Component
public class MyPermissionAdapter implements PermissionPort { ... }
```

> 如果使用方未提供 `UserAuthenticationPort` 或 `PermissionPort` 的 Bean，Starter 启动时会因构造器注入失败而报错——这是**预期行为**，强制使用方实现契约。

### 6.9 迁移路径

```
阶段 1：端口/适配器解耦（第四节/第五节）
    ↓
阶段 2：枚举下沉（SocialPlatformEnum → youlai-common）
    ↓
阶段 3：创建 youlai-security-spring-boot-starter 模块
    ↓
阶段 4：将 framework/security 代码迁移至 Starter 模块
    ↓
阶段 5：编写自动配置类 + SPI 文件
    ↓
阶段 6：youlai-framework 移除 security 子包，依赖改为引用 Starter
    ↓
阶段 7：youlai-system 的 Adapter 确认无变化（实现同一端口接口）
    ↓
阶段 8：集成测试 → 发布
```

### 6.10 Starter 边界界定

| 归属 Starter | 归属使用方（system 模块） | 归属 youlai-common |
|:---:|:---:|:---:|
| `port/` 端口接口 | `adapter/` 适配器实现 | `SecurityConstants` 常量 |
| `model/` 安全模型 | `SecurityConfig` Web 安全配置 | `SocialPlatformEnum` 枚举 |
| `service/` SecurityUserDetailsService、PermissionService | `provider/` 认证 Provider | `Result` 统一响应 |
| `token/` Token 管理器 | | `enums/` 公共枚举 |
| `filter/TokenAuthenticationFilter` | `filter/CaptchaValidationFilter` | |
| `exception/` 安全异常 | `AuthenticationEntryPoint`、`AccessDeniedHandler` | |
| `util/` SecurityUtils | | |
| `autoconfigure/` 自动配置 | | |

> **注意**：`SecurityConfig`（`@EnableWebSecurity` 配置类）归使用方，因为不同项目的安全规则（放行路径、CORS 等）不同，不应由 Starter 强制装配。Starter 仅提供 `SecurityProperties` 供使用方读取配置。

> **异常响应边界**：Starter 不提供 `MyAuthenticationEntryPoint`、`MyAccessDeniedHandler`、`ResponseWriter`。`TokenAuthenticationFilter` 只负责解析 Token 和填充 `SecurityContext`；无效 Token 抛出 Spring Security `AuthenticationException`。最终 HTTP 状态码、JSON 结构、错误码由使用方的 `AuthenticationEntryPoint` 和 `AccessDeniedHandler` 决定。

### 6.11 实操步骤（命令行操作）

> 以下以多模块项目 `youlai-boot-multi` 为例，演示从零创建 Starter 模块到代码迁移的完整命令行操作。单模块项目参考第四节先完成端口解耦，再按 6.11.3 起的步骤操作。

#### 6.11.1 重命名 UserAuthInfo → SecurityUser

```powershell
# Windows PowerShell
cd d:\Project\youlai-admin\youlai-boot-multi\youlai-framework

# 重命名文件
Rename-Item -Path "src\main\java\com\youlai\boot\framework\security\model\UserAuthInfo.java" `
            -NewName "SecurityUser.java"
```

```bash
# Linux/macOS
cd d:/Project/youlai-admin/youlai-boot-multi/youlai-framework
mv src/main/java/com/youlai/boot/framework/security/model/UserAuthInfo.java \
   src/main/java/com/youlai/boot/framework/security/model/SecurityUser.java
```

然后用 IDE 全局替换（Ctrl+Shift+R）：
- 类名 `UserAuthInfo` → `SecurityUser`
- 检查所有 `import ...UserAuthInfo` → `import ...SecurityUser`
- 更新 Javadoc 中的 `@author` 等引用

#### 6.11.2 创建 Starter Maven 模块

```powershell
# 在多模块项目根目录下创建新模块目录
cd d:\Project\youlai-admin\youlai-boot-multi

mkdir youlai-security-spring-boot-starter
mkdir youlai-security-spring-boot-starter\src\main\java\com\youlai\boot\framework\security
mkdir youlai-security-spring-boot-starter\src\main\resources\META-INF\spring
```

创建 `youlai-security-spring-boot-starter/pom.xml`（内容见 6.3 节）。

#### 6.11.3 在根 POM 注册新模块

```xml
<!-- youlai-boot-multi/pom.xml 的 <modules> 中添加 -->
<module>youlai-security-spring-boot-starter</module>
```

#### 6.11.4 迁移代码到 Starter 模块

```powershell
# 将 framework/security 下的代码移动到 Starter 模块
$src = "youlai-framework\src\main\java\com\youlai\boot\framework\security"
$dst = "youlai-security-spring-boot-starter\src\main\java\com\youlai\boot\framework\security"

# 移动 Starter 内核子包
Move-Item -Path "$src\port" -Destination "$dst\port" -Force
Move-Item -Path "$src\model" -Destination "$dst\model" -Force
Move-Item -Path "$src\service" -Destination "$dst\service" -Force
Move-Item -Path "$src\token" -Destination "$dst\token" -Force
Move-Item -Path "$src\exception" -Destination "$dst\exception" -Force
Move-Item -Path "$src\util" -Destination "$dst\util" -Force

# 只迁移 TokenAuthenticationFilter，CaptchaValidationFilter 留在使用方
mkdir "$dst\filter"
Move-Item -Path "$src\filter\TokenAuthenticationFilter.java" -Destination "$dst\filter\TokenAuthenticationFilter.java" -Force

# 只迁移 SecurityProperties，PasswordEncoder 由 SecurityAutoConfiguration 提供
mkdir "$dst\config"
Move-Item -Path "$src\config\SecurityProperties.java" -Destination "$dst\config\SecurityProperties.java" -Force

# 新建 autoconfigure 包
mkdir "$dst\autoconfigure"

# 移动 resources
Move-Item -Path "youlai-framework\src\main\resources\..." `
          -Destination "youlai-security-spring-boot-starter\src\main\resources\..." -Force
```

> **关键**：`provider/`、`handler/`、`CaptchaValidationFilter`、`PasswordEncoderConfig` **不迁移**。Provider 和验证码属于使用方业务流程；handler 和 `ResponseWriter` 属于使用方响应格式；PasswordEncoder Bean 由 `SecurityAutoConfiguration` 统一提供。

#### 6.11.5 创建自动配置类和 SPI 文件

```powershell
# 创建 3 个自动配置类（内容见 6.4 节）
# SecurityAutoConfiguration.java
# JwtTokenAutoConfiguration.java
# RedisTokenAutoConfiguration.java

# 创建 SPI 注册文件
$spiPath = "youlai-security-spring-boot-starter\src\main\resources\META-INF\spring\org.springframework.boot.autoconfigure.AutoConfiguration.imports"
Set-Content -Path $spiPath -Encoding UTF8 -Value @"
com.youlai.boot.framework.security.autoconfigure.SecurityAutoConfiguration
com.youlai.boot.framework.security.autoconfigure.JwtTokenAutoConfiguration
com.youlai.boot.framework.security.autoconfigure.RedisTokenAutoConfiguration
"@
```

#### 6.11.6 修改 youlai-framework 的 pom.xml

```xml
<!-- youlai-framework/pom.xml：添加 Starter 依赖，移除已迁移的 security 相关依赖 -->
<dependencies>
    <dependency>
        <groupId>com.youlai</groupId>
        <artifactId>youlai-common</artifactId>
    </dependency>

    <!-- 引入 Security Starter（替代原来散落的 security 代码） -->
    <dependency>
        <groupId>com.youlai</groupId>
        <artifactId>youlai-security-spring-boot-starter</artifactId>
    </dependency>

    <!-- 移除 spring-boot-starter-security（已由 Starter 传递） -->
    <!-- 移除 spring-boot-starter-data-redis（已由 Starter 传递） -->
</dependencies>
```

#### 6.11.7 枚举下沉

```powershell
# 将 SocialPlatformEnum 从 youlai-system 移到 youlai-common
$src = "youlai-system\src\main\java\com\youlai\boot\system\enums\SocialPlatformEnum.java"
$dst = "youlai-common\src\main\java\com\youlai\boot\common\enums\SocialPlatformEnum.java"

# 确保目标目录存在
mkdir "youlai-common\src\main\java\com\youlai\boot\common\enums" -Force
Move-Item -Path $src -Destination $dst -Force
```

然后用 IDE 全局替换包名：
- `com.youlai.boot.system.enums.SocialPlatformEnum` → `com.youlai.boot.common.enums.SocialPlatformEnum`

#### 6.11.8 验证编译

```powershell
cd d:\Project\youlai-admin\youlai-boot-multi

# 编译全部模块
mvn clean compile -pl youlai-security-spring-boot-starter,youlai-common,youlai-system,youlai-framework -am

# 确认 Starter 模块零 system 依赖（搜索结果应为空）
Select-String -Path "youlai-security-spring-boot-starter\src\**\*.java" `
              -Pattern "import com\.youlai\.boot\.system\." -SimpleMatch
```

> 如果最后一条命令有输出，说明 Starter 仍存在 system 依赖，需排查并消除。

#### 6.11.9 验证自动装配

```powershell
# 启动应用，观察日志中是否出现自动配置加载信息
mvn spring-boot:run -pl youlai-application

# 或在测试中验证
mvn test -pl youlai-application -Dtest=SecurityAutoConfigurationTest
```

---

### 6.12 发布到 Maven Central

> 以下流程基于 Sonatype Central Publisher Portal。发布凭据使用 Portal User Token，不使用旧发布流程。

#### 6.12.1 前置准备

| 准备项 | 说明 | 获取方式 |
|--------|------|----------|
| **Sonatype 账号** | 发布构件的账号 | 注册 https://central.sonatype.com |
| **GPG 密钥** | 签名构件 | `gpg --gen-key` 生成 |
| **Namespace 所有权验证** | 证明你拥有该 groupId | 在 Central Portal 添加 namespace 并按提示完成 DNS 或 GitHub 验证 |
| **Maven settings.xml** | 配置 Central Portal User Token | `~/.m2/settings.xml` |

#### 6.12.2 生成并上传 GPG 密钥

```bash
# 生成 GPG 密钥
gpg --gen-key
# 按提示输入姓名、邮箱、密码

# 查看密钥 ID
gpg --list-keys
# 输出示例：rsa3072/ABCDEF1234567890

# 上传公钥到密钥服务器（Maven Central 要求）
gpg --keyserver keyserver.ubuntu.com --send-keys ABCDEF1234567890
# 备用服务器
gpg --keyserver keys.openpgp.org --send-keys ABCDEF1234567890
gpg --keyserver pgp.mit.edu --send-keys ABCDEF1234567890
```

#### 6.12.3 配置 settings.xml

```xml
<!-- ~/.m2/settings.xml -->
<settings>
  <servers>
    <server>
      <id>central</id>
      <username>你的 Central Portal token username</username>
      <password>你的 Central Portal token password</password>
    </server>
  </servers>

  <profiles>
    <profile>
      <id>central</id>
      <properties>
        <gpg.executable>gpg</gpg.executable>
        <gpg.passphrase>你的GPG密码</gpg.passphrase>
      </properties>
    </profile>
  </profiles>

  <activeProfiles>
    <activeProfile>central</activeProfile>
  </activeProfiles>
</settings>
```

#### 6.12.4 配置 Starter 的 pom.xml（发布相关）

在 `youlai-security-spring-boot-starter/pom.xml` 的 `<project>` 下追加：

```xml
<!-- 发布信息 -->
<name>youlai Security Spring Boot Starter</name>
<description>认证鉴权 Spring Boot Starter，支持 JWT / Redis Token 双模式</description>
<url>https://github.com/youlaitech/youlai-starter</url>

<licenses>
  <license>
    <name>The Apache Software License, Version 2.0</name>
    <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
  </license>
</licenses>

<developers>
  <developer>
    <id>youlai</id>
    <name>youlai</name>
    <email>youlai@example.com</email>
    <url>https://github.com/youlaitech</url>
  </developer>
</developers>

<scm>
  <url>https://github.com/youlaitech/youlai-starter</url>
  <connection>scm:git:git://github.com/youlaitech/youlai-starter.git</connection>
  <developerConnection>scm:git:ssh://github.com/youlaitech/youlai-starter.git</developerConnection>
</scm>

<!-- 发布插件 -->
<build>
  <plugins>
    <!-- 源码包 -->
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-source-plugin</artifactId>
      <version>3.3.1</version>
      <executions>
        <execution>
          <id>attach-sources</id>
          <goals><goal>jar-no-fork</goal></goals>
        </execution>
      </executions>
    </plugin>

    <!-- Javadoc 包 -->
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-javadoc-plugin</artifactId>
      <version>3.6.3</version>
      <executions>
        <execution>
          <id>attach-javadocs</id>
          <goals><goal>jar</goal></goals>
        </execution>
      </executions>
    </plugin>

    <!-- GPG 签名 -->
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-gpg-plugin</artifactId>
      <version>3.1.0</version>
      <executions>
        <execution>
          <id>sign-artifacts</id>
          <phase>verify</phase>
          <goals><goal>sign</goal></goals>
        </execution>
      </executions>
    </plugin>

    <!-- 发布到 Sonatype Central Portal -->
    <plugin>
      <groupId>org.sonatype.central</groupId>
      <artifactId>central-publishing-maven-plugin</artifactId>
      <version>0.11.0</version>
      <extensions>true</extensions>
      <configuration>
        <publishingServerId>central</publishingServerId>
        <autoPublish>true</autoPublish>
      </configuration>
    </plugin>
  </plugins>
</build>
```

> **注意**：Central Portal 要求发布包包含 sources、javadocs、GPG 签名和完整 POM 元信息。`central-publishing-maven-plugin` 负责上传与发布，不负责自动补齐这些元信息。

#### 6.12.5 执行发布

```bash
cd d:/Project/youlai-admin/youlai-boot-multi/youlai-security-spring-boot-starter

# 1. 编译 + 测试 + 签名 + 发布（一条命令）
mvn clean deploy -P central

# 2. 发布后登录 https://central.sonatype.com 检查状态
#    状态变为 "Published" 后，约 15-30 分钟同步到 Maven Central
```

```bash
# 验证已发布（等待同步后）
# 浏览器访问：
# https://repo1.maven.org/maven2/com/youlai/youlai-security-spring-boot-starter/
```

#### 6.12.6 快照版本（SNAPSHOT）发布

开发阶段可发布 SNAPSHOT 版本供其他项目测试：

```xml
<!-- pom.xml 版本号带 -SNAPSHOT -->
<version>4.3.2-SNAPSHOT</version>
```

```bash
# SNAPSHOT 发布到 Central Portal Snapshots
mvn clean deploy -P central

# 使用方添加快照仓库
```

```xml
<!-- 使用方 pom.xml -->
<repositories>
  <repository>
    <id>central-snapshots</id>
    <url>https://central.sonatype.com/repository/maven-snapshots/</url>
    <snapshots><enabled>true</enabled></snapshots>
  </repository>
</repositories>
```

---

### 6.13 使用方式（其他项目接入）

#### 6.13.1 引入依赖

```xml
<!-- 使用方 pom.xml -->
<dependencies>
  <!-- 引入 youlai Security Starter -->
  <dependency>
    <groupId>com.youlai</groupId>
    <artifactId>youlai-security-spring-boot-starter</artifactId>
    <version>1.0.0</version>
  </dependency>
</dependencies>
```

#### 6.13.2 配置 application.yml

```yaml
security:
  enabled: true
  session:
    type: jwt                  # jwt | redis-token
    access-token-time-to-live: 7200
    refresh-token-time-to-live: 604800
    jwt:
      secret-key: ${JWT_SECRET:your-strong-secret-key-here}
    redis-token:
      allow-multi-login: true
  ignore-urls:
    - /api/v1/auth/login/**
    - /api/v1/auth/refresh-token
  unsecured-urls:
    - /doc.html
    - /swagger-ui/**
    - /v3/api-docs/**
```

#### 6.13.3 实现适配器（必须）

```java
// 使用方必须提供两个适配器 Bean，否则启动失败
@Component
public class MyUserAuthenticationAdapter implements UserAuthenticationPort {

    @Autowired
    private MyUserService myUserService;  // 使用方自己的用户服务

    @Override
    public SecurityUser getAuthInfoByUsername(String username) {
        // 调用使用方自己的用户查询逻辑
        MyUserEntity user = myUserService.findByUsername(username);
        if (user == null) return null;

        SecurityUser securityUser = new SecurityUser();
        securityUser.setUserId(user.getId());
        securityUser.setUsername(user.getUsername());
        securityUser.setPassword(user.getPassword());
        securityUser.setStatus(user.getStatus());
        securityUser.setDeptId(user.getDeptId());
        securityUser.setRoles(user.getRoleCodes());
        securityUser.setDataScopes(user.getDataScopes());
        return securityUser;
    }

    @Override
    public SecurityUser getAuthInfoByMobile(String mobile) {
        // ... 实现手机号查询
    }

    @Override
    public SecurityUser getAuthInfoByOpenid(SocialPlatformEnum platform, String openid) {
        // ... 实现第三方登录查询（如不支持可 return null）
    }
}

@Component
public class MyPermissionAdapter implements PermissionPort {

    @Autowired
    private MyRoleMenuService roleMenuService;

    @Override
    public Set<String> getRolePerms(Set<String> roleCodes) {
        return roleMenuService.findPermsByRoleCodes(roleCodes);
    }
}
```

#### 6.13.4 编写 SecurityConfig（使用方自定义安全规则）

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private TokenAuthenticationFilter tokenAuthenticationFilter;  // Starter 提供

    @Autowired
    private AuthenticationEntryPoint authenticationEntryPoint;    // 使用方提供

    @Autowired
    private AccessDeniedHandler accessDeniedHandler;              // 使用方提供

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/login").permitAll()
                .requestMatchers("/api/v1/auth/captcha").permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )
            // 必须位于 ExceptionTranslationFilter 之后、AuthorizationFilter 之前，
            // 这样 TokenAuthenticationFilter 抛出的 AuthenticationException 会由
            // AuthenticationEntryPoint 统一处理。
            .addFilterBefore(tokenAuthenticationFilter, AuthorizationFilter.class);
        return http.build();
    }
}
```

使用方需要提供 `AuthenticationEntryPoint` 和 `AccessDeniedHandler` Bean。youlai 项目可以在这两个 Bean 内继续使用自己的 `ResponseWriter` 输出统一 `Result`；其他项目可以返回空 401/403、Problem Details 或自有 JSON。Starter 不关心响应格式。

#### 6.13.5 验证接入成功

```java
// 启动应用，日志应显示自动配置已加载：
// ... SecurityAutoConfiguration matched
// ... JwtTokenAutoConfiguration matched（或 RedisTokenAutoConfiguration）

// 调用登录接口测试
// POST /api/v1/auth/login → 返回 token
// GET /api/v1/sys/users（带 token + @PreAuthorize("@ss.hasPerm('sys:user:list')")）→ 200 或 403
```

---

### 6.14 迁移路径总览

```
阶段 1：P1-P3 端口/适配器解耦（第四节/第五节）
    │
    ├── UserAuthInfo 重命名为 SecurityUser
    ├── 枚举下沉 SocialPlatformEnum → youlai-common
    └── 删除空占位文件、UserSession
    │
    ↓
阶段 2：创建 Starter 模块（6.11 实操步骤）
    │
    ├── 6.11.1  重命名 UserAuthInfo → SecurityUser
    ├── 6.11.2  创建 Starter 模块 + pom.xml
    ├── 6.11.3  根 POM 注册模块
    ├── 6.11.4  迁移代码到 Starter
    ├── 6.11.5  创建自动配置类 + SPI 文件
    ├── 6.11.6  youlai-framework pom.xml 改为引用 Starter
    ├── 6.11.7  枚举下沉
    ├── 6.11.8  验证编译 + 零 system 依赖
    └── 6.11.9  验证自动装配
    │
    ↓
阶段 3：发布到 Maven Central（6.12）
    │
    ├── 6.12.1  Sonatype 账号 + GPG 密钥
    ├── 6.12.2  上传 GPG 公钥
    ├── 6.12.3  配置 settings.xml
    ├── 6.12.4  配置 pom.xml 发布插件
    ├── 6.12.5  mvn clean deploy
    └── 6.12.6  验证 Maven Central
    │
    ↓
阶段 4：其他项目接入（6.13）
    │
    ├── 6.13.1  引入依赖
    ├── 6.13.2  配置 application.yml
    ├── 6.13.3  实现适配器
    ├── 6.13.4  编写 SecurityConfig
    └── 6.13.5  验证接入
```

---

## 七、测试策略

### 7.1 单元测试

| 测试目标 | Mock 对象 | 验证点 |
|----------|----------|--------|
| `SecurityUserDetailsService` | `UserAuthenticationPort` | 返回正确 `SecurityUserDetails`；用户不存在抛 `UsernameNotFoundException` |
| `PermissionService` | `PermissionPort` | 有权限返回 true；无权限返回 false；超管放行 |
| `UserAuthenticationAdapter` | `UserService`、`UserSocialService` | 委托调用正确；返回值转换正确 |
| `PermissionAdapter` | `RoleMenuService` | 委托调用正确 |
| `RedisTokenManager` | `RedisTemplate`、`JsonMapper` | 存取 `SecurityUserDetails` 正确；`password` 为 null |
| `JwtTokenManager` | — | JWT 签发/解析 `roles` 正确；`getAuthorities()` 带 `ROLE_` 前缀 |

### 7.2 集成测试

- 登录流程：用户名密码 → 生成 token → 解析 token → 权限校验
- Redis-Token 模式：登录 → Redis 存储 → 解析还原 → 踢人
- JWT 模式：登录 → JWT 签发 → 解析 → tokenVersion 失效
- SpEL 权限：`@PreAuthorize("@ss.hasPerm('sys:user:create')")` 通过/拒绝

### 7.3 回归测试用例

- **用户认证**：正确用户名/密码登录成功；错误用户名或密码登录失败
- **用户禁用**：用户状态为禁用时，登录应被拒绝（`isEnabled()` 返回 `false`）
- **角色加载**：登录成功后，`SecurityUserDetails.getAuthorities()` 获得正确的角色列表（前缀 `ROLE_`）
- **权限校验**：`@PreAuthorize` 或 `ss.hasPerm` 表达式，有权限放行、无权限拒绝
- **微信登录**：通过 `UserAuthenticationPort.getAuthInfoByOpenid` 测试社交登录
- **边界情况**：角色集合为空、权限集合为空、端口实现抛异常
- **事务和并发**：高并发登录、权限检查，确保端口实现线程安全
- **登录态失效**：发布后旧 JWT / Redis Token 均返回 401，前端跳转登录页

---

## 八、实施阶段

| 阶段 | 内容 | 风险 | 可独立发布 |
|------|------|------|:---:|
| **P0** | 多模块 parent 版本统一；确认 `SocialPlatformEnum` 下沉；确认旧 Token 发布失效策略 | 低（前置整理） | ✓ |
| **P1** | `SecurityUserDetails` 改 `roles` + 删 `UserSession` + `RedisTokenManager` 简化 + `SecurityUtils` 简化 + 删除 4 个空占位文件 | 低（内部改造，接口不变） | ✓ |
| **P2** | `JwtTokenManager` 适配 `roles` | 中（需回归 JWT 登录流程） | ✓ |
| **P3** | 端口/适配器解耦 + 枚举下沉 | 中（影响面大，需全量回归） | ✓ |
| **P4** | Security Starter 抽离（6.11 实操） | 高（模块拆分，需完整回归） | ✓ |
| **P5** | 发布到 Maven Central（6.12） | 中（发布流程，不影响代码） | ✓ |

> P0 必须先完成。P1、P2 可合并发布（模型改造一起做），P3 单独发布，P4 在 P3 稳定后执行，P5 在 P4 验证通过后执行。

---

## 九、AI 协作指南

> **本节为 AI 修改 security 代码的操作规范。任何 AI 在修改前必须阅读。**

### 9.1 修改前必读检查

```
□ 已阅读第〇节「文档使用指南」
□ 已阅读第二节「设计决策」，确认不违背 D1-D8
□ 已确认当前代码处于哪个实施阶段（P0/P1/P2/P3/P4/P5）
□ 已确认是单模块还是多模块项目
```

### 9.2 代码修改检查清单

```
□ framework/security 下的代码不含 import com.youlai.boot.system.*
□ 端口接口在 framework.security.port 包，无 system 依赖
□ 适配器在 system.security.adapter 包，标注 @Component
□ 端口返回模型使用 SecurityUser（由 UserAuthInfo 重命名）
□ SecurityUserDetails.roles 为 Set<String>，getAuthorities() 实时计算
□ UserSession.java 已删除，无残留引用
□ RedisTokenManager 存取 SecurityUserDetails，password 置 null
□ JwtTokenManager parseToken 设 roles，JWT claims 格式不变
□ SecurityUtils.getRoles() 直接取 roles 字段，无 ROLE_ strip
□ 所有新增接口和类有 Javadoc 注释
□ 端口接口 @see 注释引用适配器类（方便定位实现）
□ 多模块：youlai-framework/pom.xml 不依赖 youlai-system
```

### 9.3 新增端口/适配器决策树

```
需要安全模块调用 system 模块的新功能？
│
├─ 是 → 是否能通过现有端口完成？
│       │
│       ├─ 能 → 在现有端口接口添加方法，适配器实现
│       └─ 不能 → 需要新端口
│               │
│               ├─ 功能是否属于认证鉴权最小内核？
│               │   ├─ 是 → 新增核心端口（framework.security.port）
│               │   └─ 否 → 不加入 Security Starter，由业务模块自行实现
│               │
│               └─ 命名：<领域意图>Port，如 AuditLogPort
│
└─ 否 → 不需要端口，直接在 framework 内实现
```

### 9.4 常见问题

**Q: 为什么不用 `@Autowired` 而用构造器注入？**
A: 构造器注入（`@RequiredArgsConstructor` + `final`）保证依赖不可变、非空，符合 Spring 官方推荐。

**Q: 端口接口需要标注 `@Component` 吗？**
A: 不需要。端口是接口，由适配器实现类标注 `@Component`，Spring 自动按类型注入。

**Q: `SocialPlatformEnum` 为什么必须下沉到 common？**
A: 端口接口 `UserAuthenticationPort` 引用了它。如果端口在 Starter 模块而枚举在 system 模块，Starter 编译期就会依赖 system——违背解耦目标。

**Q: 可以在端口接口返回 system 实体类吗？**
A: **禁止**。端口返回类型必须为 `SecurityUser`（纯 POJO）或 JDK 基础类型（`Set<String>` 等）。适配器负责实体 → POJO 转换。

**Q: 为什么 `UserAuthInfo` 要重命名为 `SecurityUser`？**
A: 两点原因：① 语义——`UserAuthInfo` 偏向"认证信息"，但该模型实际承载用户安全数据（角色、权限范围、部门等），`SecurityUser` 更准确；② 作为 Starter 公开 API，`SecurityUser` 比 `UserAuthInfo` 更中性、更规范，其他项目接入时更直观。重命名而非新建，改动最小。

**Q: `SecurityUser` 和 `SecurityUserDetails` 有什么区别？**
A: `SecurityUser` 是端口返回的安全数据 POJO（含 status、nickname 等原始字段）；`SecurityUserDetails` 是 Spring Security `UserDetails` 实现（含 enabled、roles 等认证字段）。`SecurityUserDetails` 由 `SecurityUser` 构造，数据流：`Adapter 查 DB → SecurityUser → SecurityUserDetails`。

**Q: 新增了一个安全过滤器，放在哪？**
A: 放在 `framework/security/filter/`（单模块）或 Starter 的 `filter/` 包。过滤器属于框架基础设施，不依赖 system。

**Q: `SecurityConfig` 应该放在哪？**
A: 放在使用方（system 模块或 application 模块）。不同项目安全规则不同，不应由 Starter 强制装配。Starter 仅提供 `SecurityProperties`。

**Q: 如何确认重构后 framework 层零 system 依赖？**
A: 在 `framework/security` 目录执行全局搜索 `import com.youlai.boot.system`，结果应为空（`SocialPlatformEnum` 下沉后）。

### 9.5 文档修改规范

1. **修改本文档时**：更新顶部「最后更新」日期，并在附录 B 变更日志追加记录
2. **新增设计决策时**：在第〇节 0.3 关键决策表追加 `D7`、`D8`... 编号，不得修改已有决策编号
3. **新增端口时**：更新第二节 2.2 端口定义表、第六节 6.10 边界界定表
4. **完成实施阶段时**：在第八节对应阶段标注「✅ 已完成」及日期
5. **代码示例更新时**：保持与实际代码同步，注释标注 `// 改动点：` 说明差异

### 9.6 AI 提示词模板

当需要让 AI 执行 security 重构时，可使用以下提示词：

```
请按照 docs/security-refactor-plan.md 执行 security 模块重构。

当前阶段：P3（端口/适配器解耦）
项目类型：单模块（youlai-boot）

要求：
1. 先阅读第〇节「文档使用指南」和第二节「设计决策」
2. 按第四节「单模块重构」执行
3. 完成后对照第十节「代码审查清单」自查
4. 不违背 D1-D8 任何决策
5. 删除 4 个空占位文件（见 0.4 节）
```

---

## 十、代码审查清单

### 10.1 模型改造（P1/P2）

- [ ] `SecurityUserDetails` 的 `roles` 字段为 `Set<String>`，`getAuthorities()` 实时计算
- [ ] `UserSession.java` 已删除，无残留引用
- [ ] `RedisTokenManager` 存取 `SecurityUserDetails`，`password` 置 null
- [ ] `JwtTokenManager` 的 `parseToken` 设 `roles`，JWT claims 格式不变
- [ ] `SecurityUtils.getRoles()` 直接取 `roles` 字段，无 `ROLE_` strip 逻辑
- [ ] 4 个空占位文件已删除（`UserAuthQueryService`、`RolePermissionService`、`WxMaUserAuthQueryService`、`WxMaBindInfo`）

### 10.2 端口/适配器解耦（P3）

- [ ] `SecurityUserDetailsService` 不含 `import com.youlai.boot.system.*`
- [ ] `PermissionService` 不含 `import com.youlai.boot.system.*`
- [ ] 端口接口在 `framework.security.port` 包下，无 system 依赖
- [ ] 适配器在 `system.security.adapter` 包下，标注 `@Component`
- [ ] 端口接口有 `@see` 注释引用适配器类
- [ ] `SocialPlatformEnum` 已下沉到 `youlai-common`（P3/Starter 阶段）
- [ ] `SmsAuthenticationProvider`、`WxMaAuthenticationProvider` 留在使用方，未迁入 Starter

### 10.3 Starter 抽离（P4）

- [ ] `UserAuthInfo` 已重命名为 `SecurityUser`，所有引用已更新
- [ ] `youlai-security-spring-boot-starter` 模块已创建
- [ ] `pom.xml` 固定包含 Security、Web、Redis、Hutool JWT 依赖
- [ ] `AutoConfiguration.imports` 文件已创建，注册 3 个自动配置类
- [ ] `JwtTokenAutoConfiguration` 使用 `@ConditionalOnClass` + `@ConditionalOnProperty`
- [ ] `RedisTokenAutoConfiguration` 使用 `@ConditionalOnClass` + `@ConditionalOnProperty`
- [ ] `SecurityConfig` 留在使用方，不在 Starter 中强制装配
- [ ] `youlai-framework` 移除 security 子包，pom.xml 改为引用 Starter
- [ ] `SocialPlatformEnum` 已下沉到 `youlai-common`
- [ ] Starter 模块零 `import com.youlai.boot.system.*`（已验证）

### 10.4 Starter 发布

- [ ] Sonatype Central Portal 账号已注册，namespace/GroupId 已验证
- [ ] GPG 密钥已生成并上传到 3 个密钥服务器
- [ ] `settings.xml` 已配置 `central` server 凭据
- [ ] `pom.xml` 已配置 `maven-source-plugin`、`maven-javadoc-plugin`、`maven-gpg-plugin`
- [ ] `pom.xml` 已配置 `central-publishing-maven-plugin`（新方式）
- [ ] `pom.xml` 已配置 `<licenses>`、`<developers>`、`<scm>` 元信息
- [ ] `mvn clean deploy -P central` 执行成功
- [ ] Maven Central 可搜索到构件（https://central.sonatype.com）

### 10.5 通用规范

- [ ] 所有新增接口和类有 Javadoc 注释（中文）
- [ ] 端口方法签名包含 `@param`、`@return`、`@throws` 注释
- [ ] 使用构造器注入（`@RequiredArgsConstructor` + `final`）
- [ ] 日志不输出密码、密钥等敏感信息
- [ ] 单元测试覆盖端口调用和适配器委托
- [ ] 多模块：`youlai-framework/pom.xml` 不依赖 `youlai-system`
- [ ] 多模块：parent 版本与根 POM 一致（4.3.1）

---

## 附录 A：术语表

| 术语 | 含义 |
|------|------|
| **Port（端口）** | 安全模块定义的抽象接口，描述需要外部提供的功能。放在 `framework.security.port` 包。 |
| **Adapter（适配器）** | system 模块中对 Port 接口的具体实现，委托 system Service 完成实际操作。放在 `system.security.adapter` 包。 |
| **Ports & Adapters** | 六边形架构模式，核心业务通过端口与外部通信，外部实现通过适配器连入。 |
| **SecurityUser** | 安全模块的用户安全数据 POJO，纯 JDK 类型，无 system 依赖。端口接口的返回类型。由原 `UserAuthInfo` 重命名而来（D2 决策）。 |
| **SecurityUserDetails** | Spring Security `UserDetails` 实现，封装认证后的用户信息。`roles` 字段为 `Set<String>`。 |
| **UserSession** | （待删除）Redis 存储的会话中间模型，字段是 `SecurityUserDetails` 的子集。 |
| **TokenManager** | Token 管理器接口，有 `JwtTokenManager` 和 `RedisTokenManager` 两个实现。 |
| **SecurityProperties** | 安全配置属性类，`@ConfigurationProperties(prefix = "security")`。 |
| **Starter** | Spring Boot 自动装配模块，引入依赖即自动配置。本项目命名为 `youlai-security-spring-boot-starter`。 |
| **AutoConfiguration.imports** | Spring Boot 3.x+ 的自动配置注册文件，位于 `META-INF/spring/` 目录。 |
| **D1-D8** | 本文档第〇节定义的 8 条关键设计决策，不可违背。 |

---

## 附录 B：变更日志

| 日期 | 版本 | 变更内容 |
|------|------|----------|
| 2026-06-29 | v1.0 | 初版 security-refactor-plan.md（模型消除 + 单/多模块教程） |
| 2026-06-29 | v1.0 | deep-research-report.md 归档（Ports & Adapters 研究） |
| 2026-07-04 | v2.0 | **合并两文档**：消除重复逻辑（统一模型为 SecurityUser、端口收敛、重复的模式介绍）；新增第六节 Security Starter 抽离方案；新增第九节 AI 协作指南；补充空占位文件说明（0.4 节）；补充枚举下沉方案（6.7 节）；补充多模块版本不一致提醒（5.2 节） |
| 2026-07-04 | v2.1 | **SecurityUser 重命名 + Starter 实操发布指南**：D2 决策更新（`UserAuthInfo` → `SecurityUser` 重命名，非新建）；全文 `UserAuthInfo` 替换为 `SecurityUser`；新增第六节 6.11 实操步骤（命令行）；新增 Starter 发布与使用方式 |
| 2026-07-04 | v2.2 | **方案收敛为唯一施工路线**：删除 OnlineUserPort 与额外仓库分支；明确不接受旧 Token；JWT claims 统一为 `roles`；Starter 不依赖 `ResponseWriter`，异常响应交给使用方 `AuthenticationEntryPoint` / `AccessDeniedHandler`；配置项统一使用 `security.session.*` |

---

> **参考资料**：本方案基于 youlai-boot 项目 `framework/security` 与 `system` 模块的实际代码分析，结合 Ports & Adapters（六边形架构）模式和 Spring Boot Starter 自动装配规范设计。所有代码示例中的包名、类名、方法签名均与实际代码库对齐。
