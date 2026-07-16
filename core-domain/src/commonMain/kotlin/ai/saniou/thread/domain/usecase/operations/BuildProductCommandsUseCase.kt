package ai.saniou.thread.domain.usecase.operations

import ai.saniou.thread.domain.model.operations.ContentSourceKind
import ai.saniou.thread.domain.model.operations.OperationsSnapshot
import ai.saniou.thread.domain.model.operations.ProductCommandAction
import ai.saniou.thread.domain.model.operations.ProductCommandDescriptor
import ai.saniou.thread.domain.model.operations.SourceOperationalState

class BuildProductCommandsUseCase {
    operator fun invoke(snapshot: OperationsSnapshot): List<ProductCommandDescriptor> = buildList {
        if (snapshot.sources.any { it.kind == ContentSourceKind.READER && it.enabled }) {
            add(
                ProductCommandDescriptor(
                    id = "refresh:all-readers",
                    label = "刷新全部 Reader 来源",
                    description = "按照各订阅刷新策略同步文章缓存",
                    action = ProductCommandAction.REFRESH_ALL_READERS,
                )
            )
        }
        add(
            ProductCommandDescriptor(
                id = "operations:export-diagnostic",
                label = "复制脱敏运行诊断",
                description = "导出连接器状态，不包含账号、正文或本地路径",
                action = ProductCommandAction.EXPORT_DIAGNOSTIC,
            )
        )
        snapshot.sources.forEach { source ->
            if (source.enabled) {
                add(
                    ProductCommandDescriptor(
                        id = "refresh:${source.kind.name.lowercase()}:${source.id}",
                        label = "刷新 ${source.name}",
                        description = "${source.kind.name} · ${source.state.name.lowercase().replace('_', ' ')}",
                        action = ProductCommandAction.REFRESH_SOURCE,
                        sourceId = source.id,
                        sourceKind = source.kind,
                    )
                )
            }
            if (source.kind == ContentSourceKind.FORUM) {
                add(
                    ProductCommandDescriptor(
                        id = "source:${if (source.enabled) "disable" else "enable"}:${source.id}",
                        label = "${if (source.enabled) "停用" else "启用"} ${source.name}",
                        description = "更改论坛连接器在全局导航和聚合流中的可用状态",
                        action = ProductCommandAction.SET_SOURCE_ENABLED,
                        sourceId = source.id,
                        sourceKind = source.kind,
                        enabledValue = !source.enabled,
                    )
                )
            }
            if (source.kind == ContentSourceKind.FORUM &&
                "登录" in source.capabilities &&
                source.state == SourceOperationalState.AUTHENTICATION_REQUIRED
            ) {
                add(
                    ProductCommandDescriptor(
                        id = "source:login:${source.id}",
                        label = "登录 ${source.name}",
                        description = "打开该连接器的身份与凭据输入界面",
                        action = ProductCommandAction.OPEN_SOURCE_LOGIN,
                        sourceId = source.id,
                        sourceKind = source.kind,
                    )
                )
            }
        }
    }
}
