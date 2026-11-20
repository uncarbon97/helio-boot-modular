# helium-monolith

## 项目介绍
基于 Spring Boot 4.x + 虚拟线程的单体脚手架，具备多租户、国际化等特性，AI 开发友好，积累了市面上开源项目的良好实践

**JDK compatibility: 25**

【[前端演示站](https://helium-demo.uncarbon.cc/)】
【[文档站](https://helium.uncarbon.cc/)】

首次启动，需要先安装 `MySQL`/`PostgreSQL`、`Redis` 等必需中间件，详见【[快速启动文档](https://helium.uncarbon.cc/backend/getting-started/quick-start/)】

## 配套管理前端、代码生成器
| 项目名             | 简介                                                                                | Gitee                                                 | GitHub                                                  |
|-----------------|-----------------------------------------------------------------------------------|-------------------------------------------------------|---------------------------------------------------------|
| [helium-codegen](https://github.com/uncarbon-oss/helium-codegen)  | 一键生成前、后端模板代码                                                                      | [Gitee](https://gitee.com/uncarboncc/helium-codegen)  | [GitHub](https://github.com/uncarboncc/helium-codegen)  |
| [helium-admin-ui](https://github.com/uncarbon-oss/helium-admin-ui) | 基于[vue-vben-admin](https://github.com/vbenjs/vue-vben-admin) 5.X 二次开发的后台管理前端，开箱即用 | [Gitee](https://gitee.com/uncarboncc/helium-admin-ui) | [GitHub](https://github.com/uncarboncc/helium-admin-ui) |

## 工程结构
```
├───attachments  附件
│   └───db  数据库变更脚本
│       ├───MySQL          MySQL 初始化及变更脚本
│       └───PostgreSQL     PostgreSQL 初始化及变更脚本
├───bootstrap  项目主入口，负责启动 SpringBoot
├───endpoints  API 端点聚合
│   ├───admin-api  后台管理 API 端点
│   └───app-api  APP API 端点（空架子）
├───modules  业务模块层（契约 + 实现）
│   ├───commons  跨模块共享契约
│   ├───file-contract  文件管理契约
│   ├───file-service  文件管理实现
│   ├───sys-contract  系统管理契约
│   ├───sys-service  系统管理实现
│   ├───tenant-contract  多租户契约
│   └───tenant-service  多租户实现
├───integration-tests  集成测试
```
