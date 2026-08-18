
# 开放原子开源社团秘书处资料与积分管理系统

## 项目简介

本系统是开放原子开源社团秘书处的综合管理系统，涵盖成员管理、积分管理、作业管理、活动资料归档、会议纪要、财务台账、账号管理与个人资料维护等功能。

**技术栈：** Java 17 · Spring Boot 3 · PostgreSQL · Flyway · Spring Data JPA · Lombok · springdoc-openapi · Apache POI · React · TypeScript · Vite · Ant Design 5

---

## 当前运行环境

| 项目 | 配置 |
|------|------|
| 后端 | Spring Boot 3 + PostgreSQL + Flyway |
| 前端 | React + TypeScript + Vite |
| 数据库 | 本机 PostgreSQL 17 |
| 后端端口 | `8080` |
| 前端端口 | `5173` |

---

## 前置条件

| 工具 | 版本要求 | 说明 |
|------|---------|------|
| Java JDK | 17+ | 已验证 JDK 17.0.6，本机路径：`E:\tooks\JAVA-JDK17.0.6\jdk-17.0.6` |
| Maven | 3.9+ | 本机路径：`E:\tooks\apache-maven-3.9.6` |
| Node.js | 16+ | 用于运行前端开发服务器 |
| PostgreSQL | 17+ | 本地服务已启用 |

---

## 数据库说明

当前项目已切换为 PostgreSQL，不再使用 H2 文件库。

### PostgreSQL 连接信息

| 项目 | 值 |
|------|----|
| Host | `localhost` |
| Port | `5432` |
| Database | `openatom_club` |
| Username | `openatom` |
| Password | `openatom123` |

对应配置位于：`src/main/resources/application.yml`

### 说明

- PostgreSQL 不会像 H2 一样在项目目录里生成一个 `.mv.db` 文件
- 数据由 PostgreSQL 服务统一管理
- 如果你想查看数据文件位置，需要查看 PostgreSQL 自身的数据目录，而不是项目源码目录

---

## 快速启动

### 1. 启动后端

在项目根目录执行：

```cmd
E:\tooks\apache-maven-3.9.6\bin\mvn.cmd spring-boot:run
```

也可以显式指定 `JAVA_HOME`：

```cmd
set JAVA_HOME=E:\tooks\JAVA-JDK17.0.6\jdk-17.0.6 && E:\tooks\apache-maven-3.9.6\bin\mvn.cmd spring-boot:run
```

### 2. 启动前端

进入 `frontend/` 目录后执行：

```cmd
npm install
npm run dev
```

### 3. 访问系统

| 地址 | 说明 |
|------|------|
| http://localhost:5173 | 前端界面 |
| http://localhost:8080/swagger-ui.html | Swagger API 文档 |
| http://localhost:8080/v3/api-docs | API JSON 文档 |

---

## 权限系统说明

系统根据成员的 `position` 和 `department` 动态计算权限，不新增 `role` 字段。

```java
boolean fullAccess = "会长".equals(position)
                  || "副会长".equals(position)
                  || "秘书处".equals(department);
```

### 权限规则

| 条件 | 权限范围 |
|------|---------|
| 会长 | 全部权限 |
| 副会长 | 全部权限 |
| 秘书处 | 全部管理权限 |
| 部长 | 本部门作业管理（发布/批改/查看本部门提交），不拥有全权限 |
| 普通成员 | 仅可访问基础页面与个人资料 |

> 普通成员可在「我的资料」中自改部门/职务，但不能自设为管理员身份（秘书处/会长/副会长/部长）；届次只能由管理员在「成员管理」中修改。

---

## 账号与资料功能

### 已完成接口

- `GET /api/users`
- `POST /api/users`
- `POST /api/users/batch`
- `PUT /api/users/{id}/enabled`
- `POST /api/users/{id}/reset-password`
- `DELETE /api/users/{id}`
- `GET /api/my/profile`
- `PUT /api/my/profile`

### 登录返回字段

登录接口返回中包含：

- `profileCompleted`
- `initialPasswordChanged`
- `fullAccess`

### 默认账号

| 用户名 | 密码 | 说明 |
|--------|------|------|
| `lintao` | `123456` | 副会长，全权限，资料已完善 |
| `LT` | `123456` | 普通社员，用于测试普通成员权限 |

> 密码可在登录后通过右上角用户菜单 →「修改密码」自行更改。

---

## 前端页面

系统已完成全部 17 个前端页面：

| 路由 | 页面 | 权限 |
|------|------|------|
| `/login` | 登录页（深蓝宇宙星空 + Canvas 动态星场） | 公开 |
| `/` | Dashboard 首页概览（真实统计数据） | 登录即可 |
| `/members` | 成员管理（CRUD + 分页搜索 + 批量设置届次/批量删除） | 全权限 |
| `/cohorts` | 届次管理（届次 CRUD） | 全权限 |
| `/point-items` | 积分项目管理（CRUD + 启用/禁用，6 类类型） | 全权限 |
| `/my-applications` | 我的活动登记（选择成员 + 多选提交） | 登录即可 |
| `/point-applications` | 积分审核（通过/驳回 + 分页筛选） | 全权限 |
| `/points-table` | 积分总表（动态列 + 排名 + 搜索定位 + 明细管理） | 登录即可 |
| `/my-homework` | 我的作业（查看、提交、详情） | 登录即可 |
| `/homework-review` | 作业批改（列表 + 批改弹窗） | 部长 / 全权限 |
| `/homework-management` | 作业管理（CRUD + 发布/关闭） | 部长 / 全权限 |
| `/archive-links` | 活动资料归档（CRUD + 年份/类型筛选） | 登录即可 |
| `/meeting-minutes` | 会议纪要（上传/下载/编辑/删除） | 登录即可 |
| `/finance` | 财务台账（按月报表 + 凭据上传下载） | 全权限 |
| `/operation-logs` | 操作日志（分页筛选） | 全权限 |
| `/users` | 账号管理（创建/批量/启用禁用/重置密码） | 全权限 |
| `/my-profile` | 我的资料（编辑资料 + 修改密码） | 登录即可 |

### 登录页风格

- 深蓝宇宙星空背景
- 实体深色登录卡片
- 圆形社团 logo
- 克制的星空闪烁效果

### 后台风格

- 白色 / 浅灰内容区
- 标准卡片、表格、表单、Modal
- 无玻璃拟态、无霓虹光效、无粒子炫技背景

---

## 主要接口一览

| 模块 | 接口 | 方法 |
|------|------|------|
| 认证 | `/api/auth/login` | POST |
| 认证 | `/api/auth/me` | GET |
| 认证 | `/api/auth/me/password` | PUT |
| 账号管理 | `/api/users` | GET / POST |
| 账号管理 | `/api/users/batch` | POST |
| 账号管理 | `/api/users/{id}/enabled` | PUT |
| 账号管理 | `/api/users/{id}/reset-password` | POST |
| 账号管理 | `/api/users/{id}` | DELETE |
| 我的资料 | `/api/my/profile` | GET / PUT |
| 成员管理 | `/api/members` | GET / POST |
| 成员管理 | `/api/members/{id}` | GET / PUT / DELETE |
| 成员管理 | `/api/members/batch-cohort` | PUT |
| 成员管理 | `/api/members/batch-delete` | POST |
| 积分项目 | `/api/point-items` | GET / POST |
| 积分项目 | `/api/point-items/{id}` | PUT / DELETE |
| 积分申请 | `/api/point-applications` | GET / POST |
| 积分申请 | `/api/point-applications/{id}/approve` | POST |
| 积分申请 | `/api/point-applications/{id}/reject` | POST |
| 积分记录 | `/api/members/{memberId}/point-records` | GET / POST |
| 积分记录 | `/api/point-records/{id}` | PUT / DELETE |
| 积分总表 | `/api/points/table` | GET |
| 积分总表 | `/api/points/table/search-position` | GET |
| 归档链接 | `/api/archive-links` | GET / POST |
| 归档链接 | `/api/archive-links/{id}` | PUT / DELETE |
| 会议纪要 | `/api/meeting-minutes` | GET / POST |
| 会议纪要 | `/api/meeting-minutes/{id}` | PUT / DELETE |
| 会议纪要 | `/api/meeting-minutes/{id}/download` | GET |
| 财务台账 | `/api/finance/periods` | GET |
| 财务台账 | `/api/finance/{year}/{month}` | GET |
| 财务台账 | `/api/finance/{year}/{month}/report` | POST |
| 财务台账 | `/api/finance/{year}/{month}/vouchers` | POST |
| 财务台账 | `/api/finance/files/{id}/download` | GET |
| 操作日志 | `/api/operation-logs` | GET |
| 作业管理 | `/api/homeworks` | GET / POST |
| 作业管理 | `/api/homeworks/manage` | GET |
| 作业管理 | `/api/homeworks/{id}` | GET / PUT / DELETE |
| 作业管理 | `/api/homeworks/{id}/publish` | POST |
| 作业管理 | `/api/homeworks/{id}/close` | POST |
| 作业提交 | `/api/homeworks/{homeworkId}/submit` | POST |
| 作业提交 | `/api/homeworks/{homeworkId}/my-submission` | GET |
| 作业提交 | `/api/homeworks/{homeworkId}/submissions` | GET |
| 作业批改 | `/api/homework-submissions/{id}` | GET |
| 作业批改 | `/api/homework-submissions/{id}/grade` | POST |
| 作业附件 | `/api/homework-submissions/{submissionId}/files/{fileId}/download` | GET |
| 作业附件 | `/api/homework-submissions/{submissionId}/files/{fileId}/view` | GET |
| 首页统计 | `/api/dashboard/stats` | GET |

完整接口文档请访问 Swagger：http://localhost:8080/swagger-ui.html

---

## 构建与测试

### 后端测试

```cmd
E:\tooks\apache-maven-3.9.6\bin\mvn.cmd test
```

### 打包

```cmd
E:\tooks\apache-maven-3.9.6\bin\mvn.cmd package
```

### 前端构建

```cmd
cd frontend
npm run build
```

---

## 说明

- 本项目当前**不再使用 H2 作为主数据库**
- 如需本地查看前端效果，请**先启动后端，再启动前端**
- 访问前端必须通过 `http://localhost:5173`（Vite 代理转发 API 到后端），不要直接打开 `dist/index.html`
- 登录时如遇"网络错误"提示，请确认后端已启动在 `8080` 端口
- 权限测试可使用 `LT / 123456`（普通社员），功能测试可使用 `lintao / 123456`（副会长）
