# CLAUDE.md — 开放原子开源社团秘书处管理系统

## 项目身份

本项目是开放原子开源社团秘书处的综合管理系统，涵盖成员管理、积分管理、
作业管理、活动资料归档、会议纪要、财务台账、账号管理与个人资料维护等功能。

**技术栈：** Java 17 · Spring Boot 3.2.5 · PostgreSQL 17 · Flyway · Spring Data JPA ·
Lombok · springdoc-openapi · Apache POI · React 18 · TypeScript · Vite 5 · Ant Design 5

## 核心原则

1. **权限驱动**：会长/副会长/秘书处拥有全权限（`fullAccess`），普通成员受限。
2. **软删除**：所有核心表使用 `deleted_at` 字段，不做物理删除。
3. **JWT 认证**：每个请求通过 `Authorization: Bearer <token>` 携带身份，
   `JwtAuthenticationInterceptor` 实时从 Member 表读取部门/职务构造 ActorContext。
4. **BCrypt 密码**：所有密码使用 spring-security-crypto BCrypt 加密。
5. **操作日志**：敏感操作通过 `OperationLogService` 写入 `operation_logs` 表。
6. **Flyway 迁移**：数据库结构变更必须通过 `src/main/resources/db/migration/` 下的 SQL 文件。
7. **前端风格一致**：后台使用白色/浅灰内容区 + 标准 Ant Design 组件，登录页为深蓝宇宙星空风。
8. **届次体系**：以「届次（cohort）」为一级数据归属维度，成员/账号/积分/活动/作业按届隔离；
   「届次/部门/职务」三者是**组织身份**，只能由 fullAccess 管理员在成员管理中修改，普通成员不可改。

## 数据库

| 项目 | 值 |
|------|----|
| Host | `localhost` |
| Port | `5432` |
| Database | `openatom_club` |
| Username | `openatom` |
| Password | `openatom123` |

Docker Compose 位于项目根目录：`docker-compose.yml`（PostgreSQL 16 Alpine）。

### 表结构（18 张核心表）

| 表 | 说明 |
|----|------|
| `cohorts` | 届次（year UNIQUE, enabled；预置 2025/2026，软删除） |
| `members` | 成员信息（name, student_no, phone, major, department, position, **cohort_id**） |
| `users` | 登录账号（username, password_hash, member_id, enabled） |
| `point_items` | 积分项目定义（item_name, point_value, item_type, allow_member_apply） |
| `point_item_cohorts` | 积分项目适用届次关系表（point_item_id + cohort_id，0 条 = 全局适用） |
| `point_applications` | 积分申请（member_id, point_item_id, status: PENDING/APPROVED/REJECTED） |
| `point_records` | 积分流水（member_id, score, source_type: APPLICATION/MANUAL/HOMEWORK） |
| `archive_links` | 活动资料归档链接（title, archive_year, archive_type, url） |
| `archive_cohorts` | 活动归档届次关系表（archive_id + cohort_id，多对多） |
| `meeting_minutes` | 会议纪要（title, meeting_date, file_id） |
| `finance_periods` | 财务月份（finance_year, finance_month，UNIQUE 约束） |
| `finance_files` | 财务文件（period_id, file_id, file_type: REPORT/VOUCHER） |
| `files` | 文件元数据（original_name, stored_name, file_path, module_name） |
| `operation_logs` | 操作日志（operator_name, module_name, action_type, description） |
| `login_logs` | 登录日志（user_id, success, ip_address） |
| `homework_assignments` | 作业发布（title, target_type: ALL/DEPARTMENT, deadline, status, **cohort_id**） |
| `homework_submissions` | 作业提交（homework_id+member_id UNIQUE, status: SUBMITTED/GRADED, point_record_id） |
| `homework_submission_files` | 作业附件关联（submission_id, file_id） |

> `members.cohort_id` / `homework_assignments.cohort_id` 可空（历史数据 = 未分届）；新成员/新作业强制选届。

## 权限系统

没有独立的 `role` 表。权限由 `members.position` + `members.department` + `members.cohort_id` 动态计算：

```java
boolean fullAccess = "会长".equals(position)
                  || "副会长".equals(position)
                  || "秘书处".equals(department);
```

**组织身份 = 届次 + 部门 + 职务**，三者只能由 fullAccess 管理员在「成员管理」中修改；
普通成员在「我的资料」中只读展示，`PUT /api/my/profile` 的 DTO 已移除这三个字段（从接口层面杜绝自提权）。

| 条件 | 可访问页面 |
|------|-----------|
| 会长/副会长/秘书处 | 全部 17 个页面（含账号管理、财务台账、操作日志、积分审核、成员管理、作业管理、**届次管理**） |
| 普通成员 | Dashboard、我的活动登记、积分总表、活动资料归档、会议纪要、我的资料、我的作业 |

### 作业模块权限（独立于 fullAccess）

部长（`position = "部长"`）拥有独立的作业管理权限，但**不进入 fullAccess**；作业按届次绑定，核心规则是
**「部长：届次可选、部门固定本部门；fullAccess：届次可选、部门可选」**：

```java
PermissionChecker.isMinister()                   // "部长".equals(position)
PermissionChecker.canManageHomework()            // fullAccess || isMinister
PermissionChecker.canReviewSubmission(department) // fullAccess → 全部；部长 → 仅本部门
```

| 角色 | 我的作业 | 作业批改 | 作业管理 | 权限范围 |
|------|---------|---------|---------|---------|
| 普通成员 | ✅（本人届次） | ❌ | ❌ | 仅自己的提交 |
| 部长 | ✅（本人届次） | ✅ | ✅ | 届次可选，仅本部门 |
| 秘书处/会长/副会长 | ✅ | ✅ | ✅ | 届次/部门皆可选（全部） |

**后端关键类：**
- `PermissionChecker.java` — 权限校验入口（`requireFullAccess()`, `requireFinanceAccess()`, `requireLogAccess()`, `requireUserManage()`；作业专用 `requireManageHomework()` / `requireReviewSubmission()`）
- `ActorContext.java` — 每次请求实时从 Member 表构造（含 `cohortId`/`cohortYear`），权限随 member 实时变化
- `JwtAuthenticationInterceptor.java` — 拦截器，白名单 `["/api/auth/login", "/swagger-ui", "/v3/api-docs", "/h2-console", "/error"]`

**前端关键文件：**
- `src/utils/permission.ts` — `isFullAccess()`, `canManage()`, `canViewFinance()`, `canViewLogs()`, `canManageHomework()`
- `src/utils/auth.ts` — JWT token 和用户信息存储在 `localStorage`
- `src/utils/download.ts` — 文件下载/查看工具（**使用原生 fetch，不使用 axios**，见下方说明）
- `src/components/AuthGuard.tsx` — 路由级登录守卫
- `src/components/PermissionGuard.tsx` — 路由级权限守卫
- `src/layouts/MainLayout.tsx` — 侧边栏菜单根据权限动态显示/隐藏

## 快速启动

### 后端

```cmd
set JAVA_HOME=E:\tooks\JAVA-JDK17.0.6\jdk-17.0.6 && E:\tooks\apache-maven-3.9.6\bin\mvn.cmd spring-boot:run
```

后端端口：`8080`

### 前端

```cmd
cd frontend
npm install
npm run dev
```

前端端口：`5173`

### 访问地址

| 地址 | 说明 |
|------|------|
| http://localhost:5173 | 前端界面 |
| http://localhost:8080/swagger-ui.html | Swagger API 文档 |

### 默认账号

| 用户名 | 密码 | 说明 |
|--------|------|------|
| `lintao` | `123456` | 副会长，全权限，资料已完善 |
| `LT` | `123456` | 普通社员，用于测试普通成员权限 |

系统不自动创建默认账号；上表为开发环境中已存在的测试账号。

## API 接口一览

| 模块 | 端点 | 方法 |
|------|------|------|
| 届次 | `/api/cohorts` | GET / POST |
| 届次 | `/api/cohorts/{id}` | PUT / DELETE |
| 认证 | `/api/auth/login` | POST |
| 认证 | `/api/auth/me` | GET |
| 认证 | `/api/auth/me/password` | PUT |
| 账号管理 | `/api/users` | GET / POST |
| 账号管理 | `/api/users/batch` | POST |
| 账号管理 | `/api/users/{id}` | DELETE |
| 账号管理 | `/api/users/{id}/enabled` | PUT |
| 账号管理 | `/api/users/{id}/reset-password` | POST |
| 我的资料 | `/api/my/profile` | GET / PUT |
| 成员管理 | `/api/members` | GET / POST |
| 成员管理 | `/api/members/{id}` | GET / PUT / DELETE |
| 积分项目 | `/api/point-items` | GET / POST |
| 积分项目 | `/api/point-items/{id}` | PUT / DELETE |
| 积分申请 | `/api/point-applications` | GET / POST |
| 积分申请 | `/api/point-applications/my` | GET |
| 积分申请 | `/api/point-applications/{id}/approve` | POST |
| 积分申请 | `/api/point-applications/{id}/reject` | POST |
| 积分记录 | `/api/members/{memberId}/point-records` | GET / POST |
| 积分记录 | `/api/point-records/{id}` | PUT / DELETE |
| 积分总表 | `/api/points/table` | GET |
| 积分总表 | `/api/points/table/search-position` | GET |
| 归档链接 | `/api/archive-links` | GET / POST |
| 归档链接 | `/api/archive-links/{id}` | PUT / DELETE |
| 会议纪要 | `/api/meeting-minutes` | GET / POST |
| 会议纪要 | `/api/meeting-minutes/{id}` | PUT / DELETE / download GET |
| 财务台账 | `/api/finance/periods` | GET |
| 财务台账 | `/api/finance/{year}/{month}` | GET |
| 财务台账 | `/api/finance/{year}/{month}/report` | POST |
| 财务台账 | `/api/finance/{year}/{month}/vouchers` | POST |
| 财务台账 | `/api/finance/files/{id}/download` | GET |
| 财务台账 | `/api/finance/files/{id}/view` | GET（在线查看） |
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

## 项目结构

```
秘书处管理系统/
├── pom.xml                          # Maven 配置
├── docker-compose.yml               # PostgreSQL 容器
├── README.md                        # 项目说明
├── CLAUDE.md                        # 本文件
├── LOGIN_REFACTOR_SUMMARY.md        # 登录页重构历史
├── src/
│   ├── main/
│   │   ├── java/com/openatom/club/
│   │   │   ├── OpenAtomClubApplication.java
│   │   │   ├── archive/             # 归档链接模块
│   │   │   ├── auth/                # 认证模块（controller/service/entity/dto/security/config）
│   │   │   ├── cohort/              # 届次模块（entity/repository/dto/service/controller）
│   │   │   ├── common/              # 公共模块（config/exception/response/security）
│   │   │   ├── dashboard/           # 首页统计模块
│   │   │   ├── file/                # 文件存储模块
│   │   │   ├── finance/             # 财务台账模块
│   │   │   ├── homework/            # 作业管理模块（entity/repository/dto/service/controller）
│   │   │   ├── log/                 # 操作日志模块
│   │   │   ├── meeting/             # 会议纪要模块
│   │   │   ├── member/              # 成员管理模块
│   │   │   └── point/               # 积分系统模块（item/application/record/table）
│   │   └── resources/
│   │       ├── application.yml      # 主配置
│   │       └── db/migration/        # Flyway 迁移（V1~V7；V6 届次基础 + V7 届次业务范围）
│   └── test/                        # 测试
├── frontend/
│   ├── package.json
│   ├── vite.config.ts
│   ├── index.html
│   └── src/
│       ├── main.tsx                 # 入口
│       ├── App.tsx                  # Ant Design ConfigProvider + RouterProvider
│       ├── router.tsx               # 路由定义（17 个路由）
│       ├── api/                     # API 请求封装（axios 实例 + 各模块 API）
│       ├── types/                   # TypeScript 类型定义
│       ├── utils/                   # auth / permission / actor / download / cohort
│       ├── components/              # 通用组件（AuthGuard, PermissionGuard, PageContainer, LoginStarfield, CohortSelect...）
│       ├── layouts/
│       │   └── MainLayout.tsx       # 主布局（侧边栏 + 顶栏 + 用户下拉菜单）
│       ├── pages/                   # 17 个页面组件（含 homework/ 子目录 3 个作业页面 + 届次管理）
│       └── styles/                  # CSS 样式（login, dashboard, app-shell）
└── query                            # 临时的 SQL 查询文件
```

## 构建与打包

```cmd
# 后端打包
E:\tooks\apache-maven-3.9.6\bin\mvn.cmd package

# 前端构建
cd frontend && npm run build
```

## 文件上传 / 下载 / 查看机制

### 后端

- `FileStorageService.saveFile()` — 保存上传文件到 `./data/openatom-storage/{module}/{subDir}/UUID.ext`，写入 `files` 表元数据
- `FileStorageService.viewFile(fileId, response)` — 在线查看（`Content-Disposition: inline`），浏览器直接渲染
- `FileStorageService.downloadFile(fileId, response)` — 下载（`Content-Disposition: attachment`）
- `FinanceService.getMonthDetail()` — 已改为自动填充 `originalName` / `fileSize` / `contentType` 从 `files` 表到返回的 `FinanceFileResponse`

### 文件接口

| 接口 | 说明 |
|------|------|
| `GET /api/finance/files/{id}/view` | 在线查看财务文件（新标签页打开） |
| `GET /api/finance/files/{id}/download` | 下载财务文件 |
| `GET /api/meeting-minutes/{id}/download` | 下载会议纪要文件 |

### 前端下载工具 (`download.ts`)

**关键架构决策**：`download.ts` 中的 `downloadFile()` 和 `viewFile()` **必须使用原生 `fetch`**，不能使用 axios 实例。

**原因**：axios 响应拦截器（`request.ts`）假设所有响应都是 JSON 格式，会对 `response.data` 做 `code === 0` 检查。Blob 类型的响应没有 `code` 字段，会被当作错误处理，显示"请求失败"或"网络错误"。

```typescript
// ✅ 正确：使用原生 fetch
import { getToken } from './auth'
const resp = await fetch(url, {
  headers: { Authorization: `Bearer ${token}` },
})
const blob = await resp.blob()

// ❌ 错误：使用 axios（会被拦截器误判为失败）
import request from '../api/request'
const blob = await request.get(url, { responseType: 'blob' })
```

`downloadFile(url)` — 创建临时 `<a>` 触发浏览器下载
`viewFile(url)` — 创建 blob URL，`window.open` 新标签页查看

### 财务页面 UI

- **报表区**：文件名可点击 → 在线查看；显示上传时间；查看/下载/删除按钮
- **凭据表格**：文件名可点击 → 在线查看；上传时间列；查看/下载/删除按钮
- 不显示文件大小、上传人、MIME 类型等信息

## 开发注意事项

- **不要新增 role 表或 role 字段**，权限基于 `members.position` + `members.department`；届次只是数据归属维度，不改变 fullAccess 判定。
- **作业模块权限**：部长拥有独立作业管理权限（`canManageHomework()`），但**不进入 fullAccess**；**部长跨届可管理、但仅限本部门**，fullAccess 跨届跨部门；批改/下载附件需用 `canReviewSubmission(department)` 校验本部门范围。
- **届次查询参数约定**：`cohortId` 空=全部、`-1`=未分届、正数=指定届次（前端用 `Segmented`/`Select` 传 `-1` 表示未分届）。
- **组织身份只能由管理员改**：届次/部门/职务只能通过 `PUT /api/members/{id}` 由 fullAccess 修改；`PUT /api/my/profile` 的 DTO 只含 name/studentNo/phone/major。
- **届次显示**：统一 `cohortLabel(year)` → `"YYYY届"`；数据库存完整年份（`cohorts.year`），禁止存 `"2026届"` 或 `"26"`。
- **关系表**：`point_item_cohorts`（0 条 = 全局适用）和 `archive_cohorts` 存多届，禁止用 `"2025,2026"` 字符串。
- **作业届次校验**：`listMyHomework`/`getAssignment`/`submitHomework` 都按 `assignment.cohortId == actor.cohortId` 校验；部长管理走 `department == actor.department`（不限制届次）。
- **不要将 H2 用作生产数据库**（仅测试用，`ddl-auto: validate` 确保 Flyway 管理全部 schema）。
- **所有表统一使用 BIGINT IDENTITY 主键**。
- **前端不要在后台页面使用玻璃拟态、霓虹光效或粒子特效**——后台保持简洁专业的白色/浅灰风格。
- **登录页的视觉风格**（深蓝宇宙星空 + Canvas 星场 + 鼠标排斥扰动）是刻意设计，修改时注意保持一致。
- **操作日志**由 `OperationLogService` 统一写入，新增敏感操作时务必记录。
- **文件上传**通过 `FileStorageService` 统一管理，存储路径为 `./data/openatom-storage`。**文件下载/查看不经过 axios**，使用原生 `fetch`（见上方文件机制说明）。
- **密码管理**：系统不再内置默认账号；账号通过账号管理功能创建，密码由管理员设置（BCrypt 加密）。
- **积分项目 API** (`GET /api/point-items`) 返回 `List`（非分页结构），前端 API 返回类型为 `PointItem[]`，不要再用 `.list` 访问。
- **积分总表** (`PointRecordRepository`) 的统计查询需注意 `pointItemId` 可能为 NULL（手动加分记录），已用 `COALESCE` 和 `IS NOT NULL` 处理。
- **前端 API 路径**：`/api/point-applications` 的 POST 对应提交登记（非 `/api/point-applications/submit`），前端用 `submitPointApplications` 封装。
- **搜索定位 API** (`/api/points/table/search-position`) 需传中文关键词时应 URL 编码，直接拼接可能因编码问题失败。
- **前端路由**：所有 API 路径使用相对路径（如 `/api/users`），由 Vite proxy 转发到 `localhost:8080`。下载/查看接口的 URL 同样用相对路径，交给 `download.ts` 中的原生 fetch 处理。
- **MainLayout.tsx**：修改密码功能已集成在用户下拉菜单和 MyProfile 页面中，不需单独页面。登录时若 `initialPasswordChanged === false` 会被强制跳转到我的资料页要求改密。
