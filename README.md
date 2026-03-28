<div align="center">
   <img alt="logo" width="100" height="100" src="https://foruda.gitee.com/images/1733417239320800627/3c5290fe_716974.png">
   <h2>youlai-boot</h2>
   <p><b>Spring Boot 4 权限管理系统</b></p>
   <img alt="Java" src="https://img.shields.io/badge/Java-17-brightgreen.svg"/>
   <img alt="Spring Boot" src="https://img.shields.io/badge/SpringBoot-4.0.1-green.svg"/>
   <img alt="License" src="https://img.shields.io/badge/License-Apache%202.0-blue.svg"/>
   <a href="https://gitee.com/youlaiorg/youlai-boot" target="_blank">
     <img alt="Gitee" src="https://gitee.com/youlaiorg/youlai-boot/badge/star.svg"/>
   </a>
   <a href="https://github.com/haoxianrui/youlai-boot" target="_blank">
     <img alt="GitHub" src="https://img.shields.io/github/stars/haoxianrui/youlai-boot.svg?style=social&label=Stars"/>
   </a>
   <a href="https://gitcode.com/youlai/youlai-boot" target="_blank">
     <img alt="GitCode" src="https://gitcode.com/youlai/youlai-boot/star/badge.svg"/>
   </a>
</div>

<p align="center">
  <a target="_blank" href="https://vue.youlai.tech/">🖥️ 在线预览</a>
  <span>&nbsp;|&nbsp;</span>
  <a target="_blank" href="https://www.youlai.tech/youlai-boot">📑 官方文档</a>
  <span>&nbsp;|&nbsp;</span>
  <a target="_blank" href="https://www.youlai.tech">🌐 官网</a>
</p>

---

## 📢 项目简介

基于 **JDK 17 + Spring Boot 4 + Spring Security** 构建的前后端分离权限管理系统，是 [**vue3-element-admin**](https://gitee.com/youlaiorg/vue3-element-admin) 的 Java 后端实现。

### 为什么选择 youlai-boot？

| 特性              | 说明                                                  |
| ----------------- | ----------------------------------------------------- |
| 🚀 **最新技术栈** | Spring Boot 4 + JDK 17，持续跟进最新版本              |
| 🔐 **企业级认证** | Spring Security + JWT + Redis，支持令牌续期、多端互斥 |
| 🔑 **细粒度权限** | RBAC 模型，接口级 + 按钮级权限控制                    |
| 🛠️ **开箱即用**   | 用户、角色、菜单、部门、字典等核心模块                |
| 📦 **代码生成**   | 内置代码生成器，快速构建 CRUD 功能                    |
| 🌐 **完整生态**   | Web 管理前端 + 移动端配套项目，多语言后端支持         |

## 🌈 相关项目

| 项目 | 技术栈 | 说明 |
| --- | --- | --- |
| [vue3-element-admin](https://gitee.com/youlaiorg/vue3-element-admin) | Vue 3 + Element Plus | 配套前端 |
| [vue3-element-template](https://gitee.com/youlaiorg/vue3-element-template) | Vue 3 + Element Plus | 前端精简模板 |
| [youlai-boot-tenant](https://gitee.com/youlaiorg/youlai-boot-tenant) | Spring Boot 4 | 多租户 SaaS 版 |
| [youlai-boot-flex](https://gitee.com/youlaiorg/youlai-boot-flex) | Spring Boot 3 + MyBatis-Flex | MyBatis-Flex 版 |
| [youlai-app](https://gitee.com/youlaiorg/youlai-app) | Vue 3 + uni-app | 移动端应用 |

---

## 🚀 快速开始

### 环境要求

- JDK 17+
- MySQL 5.7+/8.0+
- Redis 6.0+

### 启动步骤

**1. 克隆项目**

```bash
git clone https://gitee.com/youlaiorg/youlai-boot.git
```

**2. 初始化数据库**

执行 [youlai_admin.sql](sql/mysql/youlai_admin.sql) 创建数据库和基础数据。

**3. 修改配置**

编辑 [application-dev.yml](src/main/resources/application-dev.yml)，配置 MySQL 和 Redis：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/youlai_admin # 数据库连接地址
    username: root # 数据库用户名
    password: 123456 # 数据库密码
  data:
    redis:
      host: localhost # Redis 地址
      port: 6379 # Redis 端口
```

> 💡 默认连接线上环境（仅读权限），可直接启动体验。

**4. 启动项目**

运行 [YoulaiBootApplication.java](src/main/java/com/youlai/boot/YouLaiBootApplication.java)，访问 http://localhost:8000/doc.html 查看接口文档。

---

## 📁 目录结构

```
youlai-boot
├── docker/                          # Docker 部署
├── sql/                             # 数据库脚本
├── src/main/java/com/youlai/boot/
│   ├── auth/                        # 认证授权业务
│   ├── common/                      # 全局通用（常量、枚举、工具类、统一响应结果）
│   ├── framework/                   # 底层技术基座（高内聚积木块）
│   │   ├── cache/                   # Redis/Caffeine 缓存
│   │   ├── captcha/                 # 验证码
│   │   ├── integration/             # SMS/Mail/WxMa 集成
│   │   ├── job/                     # XxlJob 定时任务
│   │   ├── mybatis/                 # 数据库/MP配置/拦截器
│   │   ├── openapi/                 # OpenAPI/Swagger 文档
│   │   ├── security/                # 鉴权过滤器/Token机制
│   │   └── web/                     # 跨域/全局异常/限流/Jackson
│   ├── module/                      # 业务模块（File、Codegen 等）
│   ├── system/                      # 核心系统模块（用户/角色/菜单/部门）
│   └── YouLaiBootApplication.java    # 启动类
└── pom.xml                          # Maven 配置
```

---

## 🐳 Docker 部署

```bash
cd docker
docker-compose up -d
```

详细文档：[部署指南](https://www.youlai.tech/docs/admin/backend/java/deploy.html)

---

## 📚 技术文档

| 文档     | 地址                                                                               |
| -------- | ---------------------------------------------------------------------------------- |
| 官方文档 | [youlai.tech](https://www.youlai.tech/youlai-boot/)                                |
| 入门指南 | [CSDN 博客](https://youlai.blog.csdn.net/article/details/145177011)                |
| 接口文档 | [Apifox](https://www.apifox.cn/apidoc/shared-195e783f-4d85-4235-a038-eec696de4ea5) |

---

## 📄 开源协议

本项目基于 [Apache 2.0](LICENSE) 协议开源，可免费用于商业项目。

---

## ✅ 项目统计

![](https://repobeats.axiom.co/api/embed/544c5c0b5b3611a6c4d5ef0faa243a9066b89659.svg)

---

## 🤝 贡献者

[![](https://contrib.rocks/image?repo=haoxianrui/youlai-boot)](https://github.com/haoxianrui/youlai-boot/graphs/contributors)

---

## 💖 技术交流

关注「有来技术」公众号，点击菜单【交流群】获取微信群二维码（为防营销广告，实属无奈，望理解）：

<div align="center">
  <img src="https://foruda.gitee.com/images/1737108820762592766/3390ed0d_716974.png" width="280">
</div>

> 二维码过期？添加微信 **`haoxianrui`**，备注「前端/后端/全栈」即可拉你入群。
