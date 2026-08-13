# 开放原子开源社团秘书处资料与积分管理系统 - 前端

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| React | 18 | UI 框架 |
| Vite | 5 | 构建工具 |
| TypeScript | 5 | 类型支持 |
| Ant Design | 5 | UI 组件库 |
| React Router | 6 | 路由 |
| Axios | 1.7 | HTTP 请求 |
| dayjs | 1.11 | 日期处理 |

---

## 项目目录结构（整体）

```
C:\Users\20976\Desktop\秘书处管理系统\     ← 项目根目录（后端）
├── pom.xml                               ← Spring Boot 后端
├── docker-compose.yml
├── src\                                  ← Java 后端源码
└── frontend\                             ← 前端目录（本项目）
    ├── package.json
    ├── vite.config.ts
    └── src\
```

> ⚠️ **注意：所有前端命令必须在 `frontend\` 目录下执行，所有后端命令必须在项目根目录下执行。**

---

## 一、启动前端

**第一步：打开命令提示符，进入前端目录**

```cmd
cd C:\Users\20976\Desktop\秘书处管理系统\frontend
```

**第二步：安装依赖（首次使用或依赖变更后执行）**

```cmd
npm install
```

**第三步：启动开发服务器**

```cmd
npm run dev
```

启动成功后访问：**http://localhost:5173**

**其他命令（在 `frontend\` 目录下执行）：**

```cmd
# 构建生产包
npm run build

# 预览生产包
npm run preview
```

---

## 二、启动后端

> 后端需要先启动 PostgreSQL，再启动 Spring Boot。前端和后端**分别开两个命令提示符窗口**。











**第一步：打开命令提示符，进入项目根目录**

```cmd
cd C:\Users\20976\Desktop\秘书处管理系统
```

**第二步：启动 PostgreSQL 数据库**

```cmd
docker compose up -d
```

**第三步：启动 Spring Boot 后端**

```cmd
E:\tooks\apache-maven-3.9.6\bin\mvn.cmd spring-boot:run
```

启动成功后后端地址：**http://localhost:8080**

Swagger 文档：**http://localhost:8080/swagger-ui.html**

> ⚠️ **常见错误**：如果在 `C:\Users\20976` 或其他目录下执行 `mvn spring-boot:run`，会报 `No plugin found for prefix 'spring-boot'`，原因是 Maven 找不到 `pom.xml`。**必须先 cd 进入项目根目录**。

---

## 三、后端地址配置

前端默认请求 `http://localhost:8080`，如需修改请编辑 `frontend\.env` 文件：

```
VITE_API_BASE_URL=http://localhost:8080
```

> 默认已配置好，后端端口未改动无需修改。

---

## 四、当前身份 Header 设置

本系统**无需登录**，通过顶部右侧"身份设置"组件模拟当前用户：

| 字段 | 请求头 | 说明 |
|------|--------|------|
| 姓名 | `X-Actor-Name` | 当前操作者姓名 |
| 部门 | `X-Actor-Department` | 填写"秘书处"可获得管理权限 |
| 职务 | `X-Actor-Position` | 会长/副会长拥有全部权限 |

**默认身份**：张三 / 秘书处 / 会长（全部权限）

**权限规则**：

| 角色 | 判断方式 | 可见菜单 |
|------|---------|---------|
| 会长 / 副会长 | Position = 会长 或 副会长 | **全部菜单** |
| 秘书处成员 | Department = 秘书处 | 除操作日志外的全部菜单 |
| 普通社员 | 其他 | 首页、成员管理、我的活动登记、积分总表、活动资料归档、会议纪要 |

身份信息保存到 `localStorage`，页面刷新后保留。

---

## 五、完整启动步骤总结

```
【窗口一】启动后端
  1. cd C:\Users\20976\Desktop\秘书处管理系统
  2. docker compose up -d
  3. E:\tooks\apache-maven-3.9.6\bin\mvn.cmd spring-boot:run

【窗口二】启动前端
  1. cd C:\Users\20976\Desktop\秘书处管理系统\frontend
  2. npm install        （首次使用）
  3. npm run dev

【浏览器】
  前端：http://localhost:5173
  后端 Swagger：http://localhost:8080/swagger-ui.html
```

---

## 六、已完成页面（第一阶段）

| 路由 | 页面 | 权限 |
|------|------|------|
| `/` | 首页概览 | 所有人 |
| `/members` | 成员管理 | 查看：所有人；增删改：秘书处+ |
| `/point-items` | 积分项目管理 | 查看：所有人；增删改：秘书处+ |
| `/my-applications` | 我的活动登记 | 所有人 |
| `/point-applications` | 积分审核 | 仅秘书处、会长、副会长 |
| `/points-table` | 积分总表 | 所有人；加分/编辑：秘书处+ |

---

## 七、已完成页面（第二阶段）

| 路由 | 页面 | 权限 |
|------|------|------|
| `/archive-links` | 活动资料归档 | 查看/打开链接：所有人；增删改：秘书处+ |
| `/meeting-minutes` | 会议纪要 | 查看/下载：所有人；上传/编辑/删除：秘书处+ |
| `/finance` | 财务台账 | 仅秘书处、会长、副会长 |
| `/operation-logs` | 操作日志 | 仅会长、副会长 |

---

## 八、活动资料归档说明

> 第一版**只保存网盘链接 URL**，不上传真实文件，不接管网盘目录结构。

- 支持年份、类型筛选（证书材料 / 活动照片 / 日常展示 / 推文截图 / 总结材料 / 其他）
- 点击"打开链接"按钮在新标签页打开网盘地址
- 普通社员只能查看和打开链接，秘书处+ 可增删改

---

## 九、文件上传说明

| 场景 | 允许格式 | 最大数量 |
|------|---------|---------|
| 会议纪要 | `.doc` `.docx` | 1 |
| 财务支出报表 | `.doc` `.docx` | 1（上传会替换旧报表） |
| 财务凭据 | `.jpg` `.jpeg` `.png` `.pdf` `.doc` `.docx` | 多个 |

- 文件类型不符时会提示错误并阻止加入列表
- 上传成功后自动清空文件列表

---

## 十、文件下载说明

- 所有文件下载**走后端 download 接口**，不暴露服务器真实路径
- 自动解析 `Content-Disposition` 响应头中的文件名（支持中文）
- 如无法解析则使用备用文件名

---

## 十一、财务台账权限说明

- 普通社员**不可访问**财务台账页面，直接访问 `/finance` 显示 403
- 普通社员**不会触发任何财务 API 请求**
- 仅秘书处成员、会长、副会长可查看、上传、下载、删除财务文件

---

## 十二、操作日志权限说明

- 仅**会长**和**副会长**可访问操作日志
- 秘书处成员和普通社员访问 `/operation-logs` 显示 403
- 操作日志为只读，不支持删除或编辑

---

## 十三、目录结构

```
frontend\                              ← 所有前端命令在此目录执行
├── .env                               ← 环境变量（VITE_API_BASE_URL）
├── .env.example                       ← 环境变量示例
├── package.json
├── vite.config.ts
├── tsconfig.json
└── src\
    ├── api\
    │   ├── request.ts                 # Axios 实例 + 拦截器
    │   ├── member.ts                  # 成员接口
    │   ├── point.ts                   # 积分接口
    │   ├── archive.ts                 # 归档链接接口
    │   ├── meeting.ts                 # 会议纪要接口
    │   ├── finance.ts                 # 财务台账接口
    │   └── log.ts                     # 操作日志接口
    ├── components\
    │   ├── ActorSettings.tsx          # 身份设置弹窗
    │   ├── PageContainer.tsx          # 页面容器
    │   └── PermissionGuard.tsx        # 权限守卫（403）
    ├── layouts\
    │   └── MainLayout.tsx             # 主布局（含全部菜单）
    ├── pages\
    │   ├── Dashboard.tsx              # 首页概览
    │   ├── Members.tsx                # 成员管理
    │   ├── PointItems.tsx             # 积分项目管理
    │   ├── MyApplications.tsx         # 我的活动登记
    │   ├── PointApplications.tsx      # 积分审核
    │   ├── PointsTable.tsx            # 积分总表
    │   ├── ArchiveLinks.tsx           # 活动资料归档
    │   ├── MeetingMinutes.tsx         # 会议纪要
    │   ├── Finance.tsx                # 财务台账
    │   └── OperationLogs.tsx          # 操作日志
    ├── types\                         # TypeScript 类型定义
    ├── utils\
    │   ├── actor.ts                   # localStorage 身份操作
    │   ├── permission.ts              # 权限判断函数
    │   └── download.ts                # 文件下载工具
    ├── router.tsx                     # 路由配置
    ├── App.tsx
    ├── main.tsx
    └── index.css
```
