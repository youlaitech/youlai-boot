<div align="center">

<img alt="youlai-boot" width="80" src="./docs/images/logo/logo.png">

# youlai-boot

**Spring Boot 4 企业级权限管理系统后端**

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.5-6DB33F?logo=spring-boot)](https://spring.io/projects/spring-boot)
[![JDK](https://img.shields.io/badge/JDK-17%2B-007396?logo=openjdk)](https://openjdk.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue?logo=apache)](LICENSE)
[![Gitee Star](https://gitee.com/youlaiorg/youlai-boot/badge/star.svg)](https://gitee.com/youlaiorg/youlai-boot/stargazers)
[![GitHub Star](https://img.shields.io/github/stars/youlaitech/youlai-boot?style=social)](https://github.com/youlaitech/youlai-boot)
[![GitCode Star](https://gitcode.com/youlai/youlai-boot/star/badge.svg)](https://gitcode.com/youlai/youlai-boot/stargazers)

</div>

![](https://foruda.gitee.com/images/1708618984641188532/a7cca095_716974.png "rainbow.png")

<div align="center">

[🖥️ 在线预览](https://vue.youlai.tech) | [📲 移动端预览](https://app.youlai.tech) | [📖 文档](https://www.youlai.tech/docs/admin/backend/java/)

</div>

## 简介

**youlai-boot** 是一套基于 Spring Boot 4 的企业级权限管理系统后端，配套前端 [vue3-element-admin](https://gitee.com/youlaiorg/vue3-element-admin) 和移动端 [youlai-app](https://gitee.com/youlaiorg/youlai-app)，并提供 **6 种语言实现**（Java / Node.js / Go / Python / PHP / C#），共享同一套 API 规范与数据库结构。适用于企业中后台管理系统的学习参考与二次开发。

## 核心特性

- 🔐 **安全体系** — Spring Security + JWT/Redis Token 双会话模式、令牌续期、多端互斥
- 🛡️ **细粒度权限** — RBAC 五级：数据 → 菜单 → 按钮 → 接口 → 字段
- ⚡ **代码生成器** — 一键生成前后端 CRUD 代码
- 📦 **模块齐全** — 用户、角色、菜单、部门、字典、文件、定时任务、消息中心、操作日志
- 🌐 **多租户 SaaS** — 数据隔离 + 租户配置，独立 [youlai-boot-tenant](https://gitee.com/youlaiorg/youlai-boot-tenant) 版本
- 🔌 **实时通信** — SSE 推送：在线用户数、字典同步、通知广播

## 技术架构

<p align="center">
  <img alt="youlai-boot 技术架构图" width="900" src="./docs/images/architecture/architecture.png">
</p>

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

1. 克隆项目：`git clone https://gitee.com/youlaiorg/youlai-boot.git`
2. 导入数据库：`sql/youlai-admin.sql`
3. 修改配置（可选，默认已配置线上只读数据源）：`src/main/resources/application-dev.yml`
4. 启动服务，访问 http://localhost:8000/doc.html

默认账号：`admin` / `123456`

**Docker 部署**：`cd deploy/docker`，然后 `docker-compose up -d`

详细指南：[部署文档](https://www.youlai.tech/docs/admin/backends/java/deploy) · [开发规范](https://www.youlai.tech/docs/admin/backends/java/dev-standards)

## 技术栈

| 技术 | 版本 | 说明 |
|:-----|:-----|:-----|
| Spring Boot | 4.0.5 | 核心框架 |
| Spring Security | 6.x | 认证授权 |
| MyBatis-Plus | 3.5.15 | ORM 框架 |
| Druid | 1.2.24 | 数据库连接池 |
| Redis + Redisson | 6.0+ / 4.1.0 | 缓存 · 会话 · 分布式锁 |
| Caffeine | 2.9.3 | 本地缓存 |
| XXL-Job | 3.2.0 | 分布式定时任务 |
| Knife4j | 4.5.0 | API 文档 |
| MapStruct | 1.6.3 | 对象映射 |
| MinIO | 8.5.10 | 对象存储 |

## 目录结构

```
youlai-boot/
├── deploy/
│   └── docker/                      # Docker 部署编排
├── docs/                            # 项目文档与图片资源
├── sql/                             # 数据库初始化脚本
├── src/main/java/com/youlai/boot/
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

| 项目 | 技术栈 | 说明 |
|:-----|:-------|:-----|
| [vue3-element-admin](https://gitee.com/youlaiorg/vue3-element-admin) | Vue 3 + Element Plus | PC 管理前端（主推） |
| [youlai-app](https://gitee.com/youlaiorg/youlai-app) | Vue 3 + UniApp | 移动端 App |

**后端**

| 项目 | 技术栈 | 说明 |
|:-----|:-------|:-----|
| [youlai-nest](https://gitee.com/youlaiorg/youlai-nest) | NestJS + TypeORM | Node.js |
| [youlai-gin](https://gitee.com/youlaiorg/youlai-gin) | Go + Gorm | Go |
| [youlai-django](https://gitee.com/youlaiorg/youlai-django) | Django + DRF | Python |
| [youlai-thinkphp](https://gitee.com/youlaiorg/youlai-thinkphp) | ThinkPHP 8 | PHP |
| [youlai-aspnet](https://gitee.com/youlaiorg/youlai-aspnet) | ASP.NET Core | C# |

> **youlai-boot** 还提供以下变种和分支版本：[多租户](https://gitee.com/youlaiorg/youlai-boot-tenant)（Spring Boot 4）· [MyBatis-Flex](https://gitee.com/youlaiorg/youlai-boot-flex)（Spring Boot 4）· [Spring Boot 3](https://gitee.com/youlaiorg/youlai-boot/tree/spring-boot-3) · [PostgreSQL](https://gitee.com/youlaiorg/youlai-boot/tree/db-pg) · [多模块](https://gitee.com/youlaiorg/youlai-boot/tree/multi-module)
>
> 六种后端共享同一套 **RESTful API 规范** 和 **数据库结构**，前端可无缝切换。

## 文档资源

| 资源 | 地址 |
|:-----|:-----|
| 📖 完整文档站 | [www.youlai.tech/docs/admin](https://www.youlai.tech/docs/admin/) |
| 🖥️ PC 端在线预览 | [vue.youlai.tech](https://vue.youlai.tech) |
| 📱 移动端在线预览 | [app.youlai.tech](https://app.youlai.tech) |
| 🔗 Apifox 接口文档 | [apifox.com](https://www.apifox.cn/apidoc/shared-195e783f-4d85-4235-a038-eec696de4ea5) |
| 🔗 本地接口文档 | [localhost:8000/doc.html](http://localhost:8000/doc.html) |

## 参与贡献

欢迎提交 Issue 和 Pull Request！详见 [贡献指南](https://www.youlai.tech/docs/admin/faq/help)。

[![Contributors](https://contrib.rocks/image?repo=haoxianrui/youlai-boot)](https://github.com/haoxianrui/youlai-boot/graphs/contributors)

## 开源协议

本项目基于 [Apache License 2.0](LICENSE) 开源，可免费用于商业项目。

---

<table align="center">
  <tr>
    <td align="center">
      <img src="./docs/images/qr/wechat-offical.png" height="180" alt="公众号「有来技术」"><br>
      <sub>公众号「有来技术」</sub>
    </td>
    <td>&nbsp;&nbsp;&nbsp;&nbsp;</td>
    <td align="center">
      <img src="./docs/images/qr/wechat-mp.jpg" height="180" alt="小程序「有来技术」"><br>
      <sub>小程序「有来技术」</sub>
    </td>
    <td>&nbsp;&nbsp;&nbsp;&nbsp;</td>
    <td align="center">
      <img src="./docs/images/qr/wechat-personal.png" height="180" alt="添加作者微信"><br>
      <sub>添加作者微信</sub>
    </td>
  </tr>
</table>

<p align="center"><em>技术交流 · 问题反馈 · 商务合作</em></p>
