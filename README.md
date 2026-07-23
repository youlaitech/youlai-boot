<div align="center">

# <img alt="youlai-boot" width="28" valign="middle" src="./docs/images/logo/logo.png"> youlai-boot

**Spring Boot 企业级权限管理系统后端**

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.5-6DB33F?logo=spring-boot)](https://spring.io/projects/spring-boot)
[![JDK](https://img.shields.io/badge/JDK-17%2B-007396?logo=openjdk)](https://openjdk.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue?logo=apache)](LICENSE)
[![Gitee Star](https://gitee.com/youlaiorg/youlai-boot/badge/star.svg)](https://gitee.com/youlaiorg/youlai-boot/stargazers)
[![GitHub Star](https://img.shields.io/github/stars/youlaitech/youlai-boot?style=social)](https://github.com/youlaitech/youlai-boot)
[![GitCode Star](https://gitcode.com/youlai/youlai-boot/star/badge.svg)](https://gitcode.com/youlai/youlai-boot/stargazers)
<a href="https://atomgit.com/youlai/youlai-boot"><img src="https://atomgit.com/youlai/youlai-boot/star/new_badge.svg" height="20" alt="AtomGit G-Star"></a>

</div>

![](https://foruda.gitee.com/images/1708618984641188532/a7cca095_716974.png "rainbow.png")

<div align="center">

[![在线预览](https://img.shields.io/badge/在线预览-2D8CF0?style=for-the-badge&logo=google-chrome&logoColor=white)](https://vue.youlai.tech)
[![移动端预览](https://img.shields.io/badge/移动端预览-19BE6B?style=for-the-badge&logo=android&logoColor=white)](https://app.youlai.tech)
[![阅读文档](https://img.shields.io/badge/阅读文档-8B5CF6?style=for-the-badge&logo=gitbook&logoColor=white)](https://www.youlai.tech/docs/server/spring-boot/)
[![English](https://img.shields.io/badge/English-00B4D8?style=for-the-badge&logo=google-translate&logoColor=white)](./README.en.md)

</div>

## 项目简介

**youlai-boot** 是一套基于 Spring Boot 的企业级权限管理系统后端，配套前端 [vue3-element-admin](https://gitee.com/youlaiorg/vue3-element-admin) 和移动端 [youlai-app](https://gitee.com/youlaiorg/youlai-app)，并提供 **7 种语言实现**（Java / Node.js / Go / Python / PHP / C# / Rust），共享同一套 API 规范与数据库结构。适用于企业中后台管理系统的学习参考与二次开发。

## 核心特性

- 🔐 **安全体系** — Spring Security + JWT/Redis Token 双会话模式、令牌续期、多端互斥
- 🛡️ **细粒度权限** — RBAC 五级：数据 → 菜单 → 按钮 → 接口 → 字段
- ⚡ **代码生成器** — 一键生成前后端 CRUD 代码
- 📦 **模块齐全** — 用户、角色、菜单、部门、字典、文件、定时任务、消息中心、操作日志
- 🌐 **多租户 SaaS** — 数据隔离 + 租户配置，独立 [youlai-boot-tenant](https://gitee.com/youlaiorg/youlai-boot-tenant) 版本
- 🔌 **实时通信** — SSE 推送：在线用户数、字典同步、通知广播

## 系统预览

**PC 端**

<table align="center">
  <tr>
    <td><img alt="PC预览1" width="400" src="./docs/images/preview/pc-01.png"></td>
    <td><img alt="PC预览2" width="400" src="./docs/images/preview/pc-02.png"></td>
  </tr>
  <tr>
    <td><img alt="PC预览3" width="400" src="./docs/images/preview/pc-03.png"></td>
    <td><img alt="PC预览4" width="400" src="./docs/images/preview/pc-04.png"></td>
  </tr>
    <tr>
    <td><img alt="PC预览3" width="400" src="./docs/images/preview/pc-05.png"></td>
    <td><img alt="PC预览4" width="400" src="./docs/images/preview/pc-06.png"></td>
  </tr>
</table>

**移动端**

<table align="center">
  <tr>
    <td><img alt="APP预览1" width="200" src="./docs/images/preview/app-01.png"></td>
    <td><img alt="APP预览2" width="200" src="./docs/images/preview/app-02.png"></td>
    <td><img alt="APP预览3" width="200" src="./docs/images/preview/app-03.png"></td>
    <td><img alt="APP预览4" width="200" src="./docs/images/preview/app-04.png"></td>
  </tr>
</table>

## 快速开始

**环境要求**：JDK 17+ · MySQL 8.0+ · Redis 6.0+

1. 克隆项目：
   ```bash
   git clone https://gitee.com/youlaiorg/youlai-boot.git
   ```

2. 导入数据库：
   ```bash
   sql/youlai-admin.sql
   ```

3. 修改配置（可选，默认已配置线上只读数据源）：
   ```bash
   src/main/resources/application-dev.yml
   ```

4. 启动服务：

   **方式一：IDEA 启动（推荐）**
   用 IDEA 打开项目，等待 Maven 依赖下载完成，运行启动类 `YouLaiBootApplication` 即可。

   **方式二：命令行启动**
   ```bash
   mvn spring-boot:run
   ```

   启动后访问 [http://localhost:8000/doc.html](http://localhost:8000/doc.html)，能打开接口文档即说明后端已正常运行。

5. 启动前端（可选）：
   如需可视化操作界面，启动配套前端 [vue3-element-admin](https://gitee.com/youlaiorg/vue3-element-admin)，访问 [http://localhost:3000](http://localhost:3000)，使用 `admin` / `123456` 登录。

> 更多内容详见官方文档：[快速开始](https://www.youlai.tech/docs/server/spring-boot/quick-start.html) · [部署指南](https://www.youlai.tech/docs/server/spring-boot/deploy.html)

## 目录结构

```
youlai-boot/
├── deploy/
│   └── docker/                      # Docker 部署编排
├── docs/                            # 项目文档与图片资源
├── sql/                             # 数据库初始化脚本
├── src/                             # 源码目录
│   ├── YouLaiBootApplication.java   # 启动类
│   ├── auth/                        # 认证授权（登录/登出/令牌）
│   ├── codegen/                     # 代码生成器
│   ├── common/                      # 公共模块（常量/枚举/统一响应）
│   ├── file/                        # 文件服务（MinIO/本地/OSS）
│   ├── framework/                   # 技术框架层
│   │   ├── apidoc/                  # OpenAPI / Knife4j
│   │   ├── cache/                   # Redis / Caffeine 缓存
│   │   ├── captcha/                 # 图形验证码
│   │   ├── integration/             # 短信 / 邮件 / 微信
│   │   ├── job/                     # XXL-Job 定时任务
│   │   ├── mybatis/                 # MyBatis-Plus 配置
│   │   ├── security/                # Security / JWT / Token
│   │   └── web/                     # 全局异常 / 跨域 / 限流
│   ├── message/                     # SSE 消息推送
│   └── system/                      # 系统业务（用户/角色/菜单/部门）
└── pom.xml                          # Maven 依赖管理
```

## 生态矩阵

**前端**

| 项目 | 技术栈 | 说明 | 更新状态 |
|:-----|:-------|:-----|:---------|
| [vue3-element-admin](https://gitee.com/youlaiorg/vue3-element-admin) | Vue 3 + Element Plus | PC 管理前端（主推） | ✅️ |
| [youlai-app](https://gitee.com/youlaiorg/youlai-app) | Vue 3 + UniApp | 移动端 App | ✅️ |

**后端**

| 项目 | 技术栈 | 说明 | 更新状态 |
|:-----|:-------|:-----|:---------|
| [youlai-boot](https://gitee.com/youlaiorg/youlai-boot) | Spring Boot + MyBatis-Plus | Java（主推） | ✅️ |
| [youlai-nest](https://gitee.com/youlaiorg/youlai-nest) | NestJS + TypeORM | Node.js | ✅️ |
| [youlai-gin](https://gitee.com/youlaiorg/youlai-gin) | Go + Gorm | Go | ✅️ |
| [youlai-django](https://gitee.com/youlaiorg/youlai-django) | Django + DRF | Python | ✅️ |
| [youlai-fastapi](https://gitee.com/youlaiorg/youlai-fastapi) | FastAPI + SQLAlchemy | Python | ✅️ |
| [youlai-laravel](https://gitee.com/youlaiorg/youlai-laravel) | Laravel + Eloquent | PHP | ✅️ |
| [youlai-think](https://gitee.com/youlaiorg/youlai-think) | ThinkPHP + ThinkORM | PHP | ✅️ |
| [youlai-aspnet](https://gitee.com/youlaiorg/youlai-aspnet) | ASP.NET Core + EF Core | C# | ✅️ |
| [youlai-axum](https://gitee.com/youlaiorg/youlai-axum) | Axum + SeaORM | Rust | ✅️ |

> 九种后端共享同一套 **RESTful API 规范** 和 **数据库结构**，前端可无缝切换。

**变种与衍生版本**

| 项目 | 基础 | 说明 | 更新状态 |
|:-----|:-----|:-----|:---------|
| [youlai-boot-tenant](https://gitee.com/youlaiorg/youlai-boot-tenant) | youlai-boot | 多租户 SaaS，租户隔离与租户配置 | ✅️ |
| [youlai-boot-flex](https://gitee.com/youlaiorg/youlai-boot-flex) | youlai-boot | 改用 MyBatis-Flex | ✅️ |
| [youlai-boot (db-pg)](https://gitee.com/youlaiorg/youlai-boot/tree/db-pg) | youlai-boot | PostgreSQL 数据库分支 | ✅️ |
| [youlai-boot (multi-module)](https://gitee.com/youlaiorg/youlai-boot/tree/multi-module) | youlai-boot | 多模块工程拆分 | ✅️ |
| [youlai-boot (spring-boot-3)](https://gitee.com/youlaiorg/youlai-boot/tree/spring-boot-3) | youlai-boot | Spring Boot 3 兼容分支 | ✅️ |
| [youlai-nest (multi-tenant)](https://gitee.com/youlaiorg/youlai-nest/tree/multi-tenant) | youlai-nest | 多租户 SaaS，租户隔离与租户配置 | ✅️ |

## 技术合作

本项目采用 [Apache License 2.0](LICENSE) 开源，可免费商用。欢迎在 [Issue](https://gitee.com/youlaiorg/youlai-boot/issues) 提交问题或反馈，也欢迎提交 [Pull Request](https://gitee.com/youlaiorg/youlai-boot/pulls) 共建。

如需技术支持、商务合作、二次开发、项目定制或私有化部署，可联系作者微信（见下方二维码）。

<table align="center">
  <tr>
    <td align="center">
      <img src="./docs/images/qrcode/wechat-official.jpg" height="180" alt="公众号「有来技术」"><br>
      <sub>公众号「有来技术」</sub>
    </td>
    <td>&nbsp;&nbsp;&nbsp;&nbsp;</td>
    <td align="center">
      <img src="./docs/images/qrcode/wechat-mp.jpg" height="180" alt="小程序「有来技术」"><br>
      <sub>小程序「有来技术」</sub>
    </td>
    <td>&nbsp;&nbsp;&nbsp;&nbsp;</td>
    <td align="center">
      <img src="./docs/images/qrcode/wechat-personal.png" height="180" alt="添加作者微信"><br>
      <sub>添加作者微信</sub>
    </td>
  </tr>
</table>

<p align="center"><em>技术交流 · 问题反馈 · 商务合作</em></p>
