# 贴吧账户与登录模块移植计划 (Revised)

## 1. 目标
完整移植 `TiebaLite` 的账户管理功能，采用 **Unified Cookie/Account Table** 设计。
将现有的 `Cookie` 表改造为通用的 `Account` 表，支持 Tieba、NMB 等多数据源的身份凭证存储。

## 2. 现状分析
*   **原项目** (`com.huanchengfly.tieba.post`):
    *   **Cookie 来源**: `AccountUtil` 从数据库读取 `bduss`，并使用 `getBdussCookie` 格式化为 Cookie 字符串。
    *   **登录流程**: Login API -> InitNickName API -> GetUserInfo API -> 存入数据库。
*   **当前项目**:
    *   **Cookie 表**: `core-data/.../Cookie.sq` 仅存储 cookie 字符串和别名，主键为 cookie，缺乏扩展性。
    *   **TiebaApi**: 已完成大部分接口移植。

## 3. 核心设计：Unified Account Table

我们将重构 `Cookie` 表，使其能存储完整的账户信息。

### 3.1 Schema 变更 (`Cookie.sq` -> `Account.sq` 概念)
由于 `Cookie.sq` 已存在，建议对其进行 schema 升级（添加字段）或重命名。鉴于 "Cookie" 名字过于具体，建议重构为 `Account` 或扩展 `Cookie`。
**方案：扩展 `Cookie.sq`** (保持文件名，逻辑上升级为 Account 表)

```sql
CREATE TABLE IF NOT EXISTS Cookie (
    id TEXT NOT NULL PRIMARY KEY,         -- UUID (新增)
    source_id TEXT NOT NULL DEFAULT 'nmb',-- 来源标识: 'tieba', 'nmb', 'discourse' (新增)
    uid TEXT,                             -- 用户ID (Tieba uid) (新增)
    cookie TEXT NOT NULL,                 -- 核心凭证 (Tieba: BDUSS, NMB: userhash)
    alias TEXT,                           -- 用户名/昵称
    avatar TEXT,                          -- 头像 URL (新增)
    extra_data TEXT,                      -- JSON, 存储 STOKEN, TBS, etc. (新增)
    sort INTEGER NOT NULL DEFAULT 0,
    is_current INTEGER NOT NULL DEFAULT 0,-- 是否为该来源的当前选中账号 (新增)
    created_at INTEGER NOT NULL,
    last_used_at INTEGER NOT NULL
);

-- 索引与查询更新...
```

### 3.2 兼容性处理
*   **NMB**: `source_id='nmb'`, `cookie`=userhash, `uid`=null/cookie.
*   **Tieba**: `source_id='tieba'`, `cookie`=BDUSS, `uid`=tieba_uid, `extra_data`={"stoken": "...", "tbs": "..."}.

## 4. 详细任务分解

### 阶段一：数据层改造 (Data Layer)

#### 4.1 修改 SQLDelight Schema
*   **Action**: 修改 `core-data/src/commonMain/sqldelight/ai/saniou/thread/db/table/Cookie.sq`。
*   **注意**: 这是一个 Breaking Change，需要更新 `UserRepositoryImpl` 和 `NmbSource` 中所有引用 `Cookie` 表的地方。

#### 4.2 实现 AccountRepository
*   **Create**: `core-data/.../repository/AccountRepositoryImpl.kt` (通用)。
*   **Interface**: `core-domain/.../repository/AccountRepository.kt`.
    *   `getAccounts(source: String): Flow<List<Account>>`
    *   `getCurrentAccount(source: String): Flow<Account?>`
    *   `addAccount(account: Account)`
    *   `switchAccount(id: String)`

#### 4.3 升级 TiebaParameterProvider
*   **Action**: 修改 `TiebaParameterProvider`，使其注入 `AccountRepository`。
*   **Logic**:
    *   `getBduss()` -> `accountRepo.getCurrentAccount("tieba")?.cookie`
    *   `getSToken()` -> `accountRepo.getCurrentAccount("tieba")?.extraData?.stoken`

### 阶段二：业务逻辑层 (Domain Layer)

#### 4.4 移植登录 UseCase
*   **Create**: `LoginTiebaUseCase`。
*   **Flow**:
    1.  API `login(username, password)` (需确认 TiebaLite 是否支持密码登录，通常是 Webview 拦截或短信)。
    2.  若为 Webview 拦截模式，UseCase 接收 `bduss` 和 `stoken`。
    3.  调用 `initNickName`, `getUserInfo` 补全信息。
    4.  调用 `AccountRepository.addAccount`。

### 阶段三：Cookie 同步机制

#### 4.5 Cookie 管理
*   **Action**: 增强 `TiebaCommonHeaderPlugin`。
*   **Logic**:
    *   读取 Provider 的 BDUSS。
    *   按照 `BDUSS=$bduss; Path=/; Domain=.baidu.com` 格式拼接 Cookie Header (参考 `AccountUtil.getBdussCookie`)。

## 5. 执行计划
1.  **Refactor Cookie Table**: 修改 `Cookie.sq`，修复编译错误 (`UserRepositoryImpl`, `NmbSource`)。
2.  **Impl Repo**: 实现通用的 `AccountRepository`。
3.  **Wire Provider**: 连接 `TiebaParameterProvider` 与 Repo。
4.  **Impl UseCase**: 实现 Tieba 登录逻辑。