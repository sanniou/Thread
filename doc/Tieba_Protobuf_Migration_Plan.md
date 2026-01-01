# Tieba Protobuf Migration Plan

## 1. 目标 (Goal)
将 `TiebaSource` 的实现从仅依赖 JSON API (`MiniTiebaApi`, `OfficialTiebaApi`) 迁移到主要依赖 Protobuf API (`OfficialProtobufTiebaApi`)，以复刻 `TiebaLite` 的完整功能 (`MixedTiebaApiImpl`)。

## 2. 核心任务 (Core Tasks)

### 2.1 基础设施搭建 (Infrastructure)
*   **Wire Plugin**: 在 `core-data` 模块中引入 `com.squareup.wire` Gradle 插件。
*   **Protocol Buffers**: 将 `TiebaLite-4.0-dev/app/src/main/protos` 下的 `.proto` 文件复制到 `core-data/src/commonMain/proto`。
*   **Generation**: 配置 Wire 任务以生成 Kotlin Multiplatform 代码。

### 2.2 API 定义更新 (API Update)
*   **OfficialProtobufTiebaApi**: 更新接口定义，将返回值和请求体从 `Unit` 或 JSON Bean 替换为生成的 Wire Protobuf 类。
*   **Serialization**: 实现或配置 Ktor/Ktorfit 的 `WireConverterFactory`，确保能正确序列化和反序列化 Protobuf 数据。

### 2.3 业务逻辑迁移 (Logic Migration)
参考 `MixedTiebaApiImpl.kt`，在 `TiebaSource` 中实现混合策略：

*   **Recommend (Forum/Feed)**:
    *   API: `OfficialProtobufTiebaApi.frsPage` (Protobuf) 或 `forumRecommend`
    *   Fallback: `MiniTiebaApi.forumRecommend` (JSON)
*   **Thread List (Forum Page)**:
    *   API: `OfficialProtobufTiebaApi.frsPage` (Protobuf)
    *   Fallback: `MiniTiebaApi.forumPage` (JSON)
*   **Topic Detail (Thread Content)**:
    *   API: `OfficialProtobufTiebaApi.pbPage` (Protobuf)
    *   Fallback: `OfficialTiebaApi.threadContent` (JSON)
*   **Comments (Floor)**:
    *   API: `OfficialProtobufTiebaApi.pbFloor` (Protobuf)
    *   Fallback: `OfficialTiebaApi.floor` (JSON)

### 2.4 数据映射 (Mapper Update)
*   更新 `TiebaMapper`，增加从 Protobuf Generated Classes 到 Domain Models (`Channel`, `Topic`, `Comment`) 的映射函数。

## 3. 详细步骤 (Detailed Steps)

### Phase 1: Environment & Proto Import
1.  修改 `core-data/build.gradle.kts` 添加 Wire 插件。
2.  复制 `TiebaLite` 的 `.proto` 文件。
3.  执行 Gradle Build 验证代码生成。

### Phase 2: API & Serialization
1.  编写 `WireResponseConverter` (适配 Ktorfit)。
2.  更新 `OfficialProtobufTiebaApi` 接口签名。

### Phase 3: Implementation - Thread List (FrsPage)
1.  实现 `TiebaSource.getTopicsPager` 使用 Protobuf API。
2.  完善 `TiebaMapper` 处理 `FrsPageResponse`。

### Phase 4: Implementation - Thread Detail (PbPage)
1.  实现 `TiebaSource.getTopicDetail` 使用 Protobuf API。
2.  完善 `TiebaMapper` 处理 `PbPageResponse`。

## 4. 关键 Proto 文件映射 (Key Proto Files)
*   `FrsPage/FrsPageRequest.proto` -> `FrsPageRequest`
*   `FrsPage/FrsPageResponse.proto` -> `FrsPageResponse`
*   `PbPage/PbPageRequest.proto` -> `PbPageRequest`
*   `PbPage/PbPageResponse.proto` -> `PbPageResponse`
*   `PbFloor/PbFloorRequest.proto` -> `PbFloorRequest`
*   `PbFloor/PbFloorResponse.proto` -> `PbFloorResponse`
*   `User/User.proto` -> `User`
*   `ThreadInfo.proto` -> `ThreadInfo`
*   `Post.proto` -> `Post`

## 5. 风险 (Risks)
*   **Wire Compatibility**: 确保 Wire 生成的代码与 Ktorfit 兼容（主要是 Serialization Converter）。
*   **Proto Dependencies**: `TiebaLite` 的 proto 文件可能存在复杂的依赖关系，需要一次性完整复制。
*   **Field Mapping**: Protobuf 字段通常是可选的 (nullable)，需要仔细处理空值。
