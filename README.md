<p align="center">
  <img alt="youlai-boot" width="120" src="https://foruda.gitee.com/images/1733417239320800627/3c5290fe_716974.png">
</p>

<h1 align="center">youlai-boot</h1>

<p align="center">
  <strong>Spring Boot 4 企业级权限管理系统后端</strong>
</p>

<p align="center">
  <a href="https://www.youlai.tech/docs/admin/backend/java/"><img src="https://img.shields.io/badge/文档-youlai.tech-blue?style=flat-square&logo=data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNTYgMjU2Ij48cGF0aCBmaWxsPSIjMzc4M2E0IiBkPSJNMjQ4IDExMUwzMSAxNDljMTQuNSA0LjkgMjkuNiAyMi41IDQ0LjIgMS44IDguNyAzLjEgMTcuNCAxIDEyLjhjLTIuOSA3LjItNi43IDEzLjUtMTIuOCAxNy40eiIvPjwvc3ZnPg==" alt="Documentation"></a>
  <a href="https://vue.youlai.tech"><img src="https://img.shields.io/badge/在线预览-vue.youlai.tech-10B981?style=flat-square&logo=data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNTYgMjU2Ij48cGF0aCBmaWxsPSIjZmZmIiBkPSJNMjQ4IDExMUwzMSAxNDljMTQuNSA0LjkgMjkuNiAyMi41IDQ0LjIgMS44IDguNyAzLjEgMTcuNCAxIDEyLjhjLTIuOSA3LjItNi43IDEzLjUtMTIuOCAxNy40eiIvPjwvc3ZnPg==" alt="Demo"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-Apache%202.0-blue?style=flat-square"></a>
</p>

<p align="center">
  <a href="https://gitee.com/youlaiorg/youlai-boot/stargazers"><img src="https://gitee.com/youlaiorg/youlai-boot/badge/star.svg?style=flat-square"></a>
  <a href="https://github.com/haoxianrui/youlai-boot"><img src="https://img.shields.io/github/stars/haoxianrui/youlai-boot?style=social&label=Star"></a>
</p>

---

> [English](#) | 简体中文

---

## 🎯 项目定位

一套 **Spring Boot 4 后端权限管理系统**，配套前端 [vue3-element-admin](https://gitee.com/youlaiorg/vue3-element-admin)，并提供 **6 种语言实现**（Java / Node.js / Go / Python / PHP / C#），共享同一套 API 规范与数据库结构。

**适合场景**：企业中后台管理系统的后端学习参考、二次开发基础脚手架。

---

## ✨ 核心能力

| 能力 | 说明 |
|------|------|
| 🔐 **安全体系** | Spring Security + JWT + Redis 多端互斥、令牌续期、验证码防刷 |
| 🛡️ **细粒度权限** | RBAC 五级：数据权限 → 菜单 → 按钮 → 接口 → 字段级 |
| ⚡ **代码生成器** | 可视化配置表单，一键生成 Entity/VO/Controller/Service/CRUD 前后端代码 |
| 📦 **模块齐全** | 用户、角色、菜单、部门、字典、文件、定时任务、消息中心、操作日志 |
| 🌐 **多租户 SaaS** | 数据隔离 + 租户配置，独立 [youlai-boot-tenant](https://gitee.com/youlaiorg/youlai-boot-tenant) 版本 |
| 🔌 **实时通信** | 内置 SSE 推送服务（在线用户数、字典同步、通知广播） |
| 📱 **生态完整** | 配套移动端 [youlai-app](https://gitee.com/youlaiorg/youlai-app)（UniApp）+ 完整[技术文档](https://www.youlai.tech/docs/admin/) |

## 📸 系统预览


<table align="center">
  <tr>
    <td><img alt="系统预览1" width="400" src="https://www.youlai.tech/storage/blog/2026/04/12/admin_preview.jpeg"></td>
    <td><img alt="系统预览2" width="400" src="https://www.youlai.tech/storage/blog/2026/04/12/441_1x_shots_so.jpeg"></td>
  </tr>
</table>


## 🚀 快速开始

### 环境要求

| 组件 | 版本 |
|------|------|
| JDK | 17+ |
| MySQL | 8.0+ / 5.7+ |
| Redis | 6.0+ |

### 本地启动

```bash
# 1. 克隆项目
git clone https://gitee.com/youlaiorg/youlai-boot.git

# 2. 导入数据库脚本 sql/mysql/youlai_admin.sql

# 3. 修改 application-dev.yml 配置 MySQL 和 Redis 连接信息
#    💡 默认已配置线上只读数据源，可直接启动体验

# 4. 运行 YouLaiBootApplication.java，访问 http://localhost:8000/doc.html
```

默认账号：`admin` / `123456`

### Docker 部署

```bash
cd docker && docker-compose up -d
```

详细指南：[部署文档](https://www.youlai.tech/docs/admin/backends/java/deploy) · [开发规范](https://www.youlai.tech/docs/admin/backends/java/dev-standards)

## 📁 目录结构

```
youlai-boot/
├── docker/                     # Docker 部署编排
├── sql/                        # 数据库初始化脚本
├── src/main/java/com/youlai/boot/
│   ├── YouLaiBootApplication   # 启动类
│   ├── auth/                    # 认证授权（登录/登出/令牌）
│   ├── codegen/                 # 代码生成器
│   ├── common/                  # 公共模块（常量/枚举/统一响应）
│   ├── file/                    # 文件服务（MinIO/本地存储）
│   ├── framework/               # 技术基座
│   │   ├── apidoc/             # OpenAPI/Swagger
│   │   ├── cache/              # Redis/Caffeine 缓存
│   │   ├── captcha/            # 图形验证码
│   │   ├── integration/        # 短信/邮件/微信
│   │   ├── job/                # XXL-Job 定时任务
│   │   ├── mybatis/            # MyBatis Plus 配置
│   │   ├── security/           # 安全过滤器/Token机制
│   │   └── web/                # 全局异常/跨域/限流
│   ├── message/                 # SSE 消息推送
│   └── system/                  # 业务模块（用户/角色/菜单/部门）
└── pom.xml                      # Maven 依赖
```

## 🌐 相关生态

| 项目 | 技术栈 | 定位 |
|------|--------|------|
| [**vue3-element-admin**](https://gitee.com/youlaiorg/vue3-element-admin) | Vue 3 + Element Plus | **PC 管理前端**（主推） |
| [**youlai-app**](https://gitee.com/youlaiorg/youlai-app) | Vue 3 + UniApp | **移动端 App** |
| [**youlai-boot-tenant**](https://gitee.com/youlaiorg/youlai-boot-tenant) | Spring Boot 4 | **SaaS 多租户版本** |
| [**youlai-boot-flex**](https://gitee.com/youlaiorg/youlai-boot-flex) | Spring Boot 3 + MyBatis-Flex | MyBatis-Flex 版 |
| [**youlai-nest**](https://gitee.com/youlaiorg/youlai-nest) | NestJS + TypeORM | **Node.js 后端** |
| [**youlai-gin**](https://gitee.com/youlaiorg/youlai-gin) | Go + Gorm | **Go 后端** |
| [**youlai-django**](https://gitee.com/youlaiorg/youlai-django) | Django + DRF | **Python 后端** |
| [**youlai-thinkphp**](https://gitee.com/youlaiorg/youlai-thinkphp) | ThinkPHP 8 | **PHP 后端** |
| [**youlai-aspnet**](https://gitee.com/youlaiorg/youlai-aspnet) | ASP.NET Core | **C# 后端** |

> 六种后端共享同一套 **RESTful API 规范** 和 **数据库结构**，前端可无缝切换。

## 📘 文档资源

| 资源 | 地址 |
|------|------|
| **📖 完整文档站** | [docs.youlai.tech](https://www.youlai.tech/docs/admin/) |
| **🖥️ 在线预览（前端）** | [vue.youlai.tech](https://vue.youlai.tech) |
| **📱 在线预览（移动端）** | [app.youlai.tech](https://app.youlai.tech) |
| **🔗 接口文档** | 启动后访问 [http://localhost:8000/doc.html](http://localhost:8000/doc.html) |

## 📊 项目统计

![Repobeats](https://repobeats.axiom.co/api/embed/544c5c0b5b3611a6c4d5ef0faa243a9066b89659.svg)

## 🤝 参与贡献

欢迎 Issue、PR 和 Star！详见 [贡献指南](https://www.youlai.tech/docs/admin/faq/help)。

[![Contributors](https://contrib.rocks/image?repo=haoxianrui/youlai-boot)](https://github.com/haoxianrui/youlai-boot/graphs/contributors)

## 📄 开源协议

本项目基于 [Apache License 2.0](LICENSE) 开源，可免费用于商业项目。

---

<div align="center">

**关注「有来技术」，获取最新动态与技术分享**

<br>

<img src="https://foruda.gitee.com/images/1737108820762592766/3390ed0d_716974.png" width="220">

<br>

*微信搜索「有来技术」或扫码关注*

</div>
