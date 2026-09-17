# helium-monolith

## 项目介绍
基于 Spring Boot 4.x，是一款预置多租户、国际化能力，支持虚拟线程的多模块单体开发脚手架，积累市面上开源项目的良好实践

JDK compatibility: 25

【[前端演示站](https://helium-demo.uncarbon.cc/)】
【[官方文档](https://helium.uncarbon.cc/)】 
【[主要技术栈依赖](https://helium.uncarbon.cc/)】
【[快速启动步骤](https://helium.uncarbon.cc/)】
【[更新记录](https://helium.uncarbon.cc/appendix/change-log)】
【[编码良好实践](https://helium.uncarbon.cc/experience/good-practices)】

需要先安装 `MySQL`/`PostgreSQL`、`Redis` 等必需中间件

基础支撑构件 [helium-starters](https://github.com/uncarboncc/helium-starters) 已推送至Maven中央仓库，加载时会自动拉取

## 配套代码生成器 & 后台管理前端
| 项目名             | 简介                                                                                | Gitee                                                 | GitHub                                                  |
|-----------------|-----------------------------------------------------------------------------------|-------------------------------------------------------|---------------------------------------------------------|
| helium-codegen  | 一键生成前、后端模板代码                                                                      | [Gitee](https://gitee.com/uncarboncc/helium-codegen)  | [GitHub](https://github.com/uncarboncc/helium-codegen)  |
| helium-admin-ui | 基于[vue-vben-admin](https://github.com/vbenjs/vue-vben-admin) 5.X 二次开发的后台管理前端，开箱即用 | [Gitee](https://gitee.com/uncarboncc/helium-admin-ui) | [GitHub](https://github.com/uncarboncc/helium-admin-ui) |

## 工程结构
```
├───attachments  附件
│   └───db  数据库变更脚本
│       ├───MySQL          MySQL 初始化及变更脚本
│       └───PostgreSQL     PostgreSQL 初始化及变更脚本
├───bootstrap  项目主入口，负责启动 SpringBoot
├───commons  跨模块共享契约
├───endpoints  API 端点聚合
│   ├───admin-api  后台管理 API 端点
│   └───app-api  APP API 端点（空架子）
├───modules  业务模块层（契约 + 实现）
│   ├───file-contract  文件管理契约
│   ├───file-impl  文件管理实现
│   ├───sys-contract  系统管理契约
│   ├───sys-impl  系统管理实现
│   ├───tenant-contract  多租户契约
│   └───tenant-impl  多租户实现
├───integration-tests  集成测试
```
