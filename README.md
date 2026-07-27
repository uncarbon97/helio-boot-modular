# helium-monolith

## 项目介绍
基于 Spring Boot 4.x，是一款预置多租户、国际化能力，支持虚拟线程的多模块单体开发脚手架，积累市面上开源项目的良好实践，适合开发者学习 JavaWeb 开发的良好实践

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
| 项目名                   | 简介                                                                                | Gitee                                                       | GitHub                                                        |
|-----------------------|-----------------------------------------------------------------------------------|-------------------------------------------------------------|---------------------------------------------------------------|
| helium-codegen        | 一键生成前、后端模板代码                                                                      | [Gitee](https://gitee.com/uncarboncc/helium-codegen)        | [GitHub](https://github.com/uncarboncc/helium-codegen)        |
| helium-ui-admin-vben5 | 基于[vue-vben-admin](https://github.com/vbenjs/vue-vben-admin) 5.X 二次开发的后台管理前端，开箱即用 | [Gitee](https://gitee.com/uncarboncc/helium-ui-admin-vben5) | [GitHub](https://github.com/uncarboncc/helium-ui-admin-vben5) |

## 工程结构
```
├───attachments  附件
│   ├───db  数据库变更脚本
│   │   ├───MySQL          MySQL 初始化及变更脚本
│   │   └───PostgreSQL     PostgreSQL 初始化及变更脚本
│   └───service-archtype   新增业务服务脚手架模板
│       ├───template-facade  门面模块模板
│       └───template-service  服务实现模块模板
├───bootstrap  【Maven模块】项目主入口，负责启动SpringBoot
│   └───src
│       └───main
│           ├───java
│           │   └───cc
│           │       └───uncarbon
│           │           └───module
│           │               ├───config       全局配置类
│           │               │   └───satoken  Sa-Token 相关配置
│           │               ├───context      上下文
│           │               └───filter       Web过滤器
│           └───resources  资源，包含符合Spring Boot标准的YAML配置文件、Logback配置文件等
│               ├───configs    汇总的YAML配置文件
│               └───ip2region  Ip2region 离线库
├───commons  【Maven模块】跨模块共享契约
│   └───src
│       └───main
│           └───java
│               └───cc
│                   └───uncarbon
│                       └───module
│                           └───commons
│                               ├───constant   常量
│                               ├───enumdict   枚举字典
│                               ├───enums      枚举
│                               ├───errorcode  错误码
│                               ├───exception  异常
│                               ├───iplocation IP归属地
│                               ├───model      抽象模型
│                               │   └───request  用于请求的
│                               └───satoken    Sa-Token 相关
├───endpoints  【Maven聚合】HTTP控制器入口层
│   ├───admin-api  【Maven模块】admin，用于系统管理的HTTP控制器
│   │   └───src
│   │       └───main
│   │           └───java
│   │               └───cc
│   │                   └───uncarbon
│   │                       └───module
│   │                           └───adminapi
│   │                               ├───annotation      自定义注解
│   │                               ├───constant        常量
│   │                               ├───controller
│   │                               │   ├───auth         登录登出接口
│   │                               │   ├───file         文件存储接口
│   │                               │   ├───selectoption 下拉选项接口
│   │                               │   ├───sys          预置系统管理接口
│   │                               │   └───tenant       租户管理接口
│   │                               ├───errorcode       错误码
│   │                               ├───event           事件
│   │                               │   └───listener     事件监听器
│   │                               ├───helper          助手类
│   │                               ├───model           抽象模型
│   │                               │   ├───internal     内部使用的
│   │                               │   └───response     用于响应的
│   │                               └───props           配置属性类
│   └───app-api  【Maven模块】app，用于C端的HTTP控制器【只是一个骨架，并没有业务实现】
│       └───src
│           └───main
│               └───java
│                   └───cc
│                       └───uncarbon
│                           └───module
│                               └───appapi
│                                   ├───config      配置类
│                                   ├───controller
│                                   └───props       配置属性类
├───services  【Maven聚合】业务服务层（门面 + 实现）
│   ├───sys  【Maven模块】预置系统管理服务
│   │   ├───sys-facade  【Maven模块】预置系统管理服务门面
│   │   │   └───src
│   │   │       └───main
│   │   │           └───java
│   │   │               └───cc
│   │   │                   └───uncarbon
│   │   │                       └───module
│   │   │                           └───sys
│   │   │                               ├───constant   常量
│   │   │                               ├───enums      枚举
│   │   │                               ├───errorcode  错误码
│   │   │                               ├───facade     门面
│   │   │                               └───model      抽象模型
│   │   │                                   ├───query     用于查询的
│   │   │                                   ├───request   用于请求的
│   │   │                                   ├───response  用于响应的
│   │   │                                   └───valueobj  值对象
│   │   └───sys-service  【Maven模块】预置系统管理服务实现
│   │       └───src
│   │           └───main
│   │               └───java
│   │                   └───cc
│   │                       └───uncarbon
│   │                           └───module
│   │                               └───sys
│   │                                   ├───biz       门面实现类
│   │                                   ├───config    配置类
│   │                                   ├───dal       数据访问层
│   │                                   │   ├───entity  实体
│   │                                   │   └───mapper  Mybatis Mapper
│   │                                   ├───helper    助手类
│   │                                   ├───model     抽象模型
│   │                                   │   └───internal  内部使用的
│   │                                   ├───resolver  解析器
│   │                                   ├───service   服务类
│   │                                   │   └───impl     服务实现类
│   │                                   └───util      静态工具类
│   ├───tenant  【Maven模块】租户服务
│   │   ├───tenant-facade  【Maven模块】租户服务门面
│   │   │   └───src
│   │   │       └───main
│   │   │           └───java
│   │   │               └───cc
│   │   │                   └───uncarbon
│   │   │                       └───module
│   │   │                           └───tenant
│   │   │                               ├───errorcode  错误码
│   │   │                               ├───facade     门面
│   │   │                               └───model      抽象模型
│   │   │                                   ├───query     用于查询的
│   │   │                                   ├───request   用于请求的
│   │   │                                   └───valueobj  值对象
│   │   └───tenant-service  【Maven模块】租户服务实现
│   │       └───src
│   │           └───main
│   │               └───java
│   │                   └───cc
│   │                       └───uncarbon
│   │                           └───module
│   │                               └───tenant
│   │                                   ├───biz       门面实现类
│   │                                   ├───dal       数据访问层
│   │                                   │   ├───entity  实体
│   │                                   │   └───mapper  Mybatis Mapper
│   │                                   ├───service   服务类
│   │                                   │   └───impl     服务实现类
│   └───file  【Maven模块】文件存储服务
│       ├───file-facade  【Maven模块】文件存储服务门面
│       │   └───src
│       │       └───main
│       │           └───java
│       │               └───cc
│       │                   └───uncarbon
│       │                       └───module
│       │                           └───file
│       │                               ├───enums      枚举
│       │                               ├───errorcode  错误码
│       │                               ├───facade     门面
│       │                               ├───model      抽象模型
│       │                               │   ├───internal  内部使用的
│       │                               │   ├───query     用于查询的
│       │                               │   ├───request   用于请求的
│       │                               │   ├───response  用于响应的
│       │                               │   ├───setting   存储配置
│       │                               │   │   └───storage 存储类型配置
│       │                               │   └───valueobj  值对象
│       │                               └───util       静态工具类
│       └───file-service  【Maven模块】文件存储服务实现
│           └───src
│               └───main
│                   └───java
│                       └───cc
│                           └───uncarbon
│                               └───module
│                                   └───file
│                                       ├───biz      门面实现类
│                                       ├───config   配置类
│                                       ├───dal      数据访问层
│                                       │   ├───entity  实体
│                                       │   └───mapper  Mybatis Mapper
│                                       └───service  服务类
│                                           └───impl     服务实现类
├───integration-tests  【Maven模块】集成测试
│   └───src
│       └───test
│           ├───java        集成测试用例
│           └───resources   集成测试资源
```
