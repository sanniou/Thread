package ai.saniou.thread.domain.usecase.operations

import ai.saniou.thread.domain.model.operations.ContentSourceKind
import ai.saniou.thread.domain.model.operations.OperationsSnapshot
import ai.saniou.thread.domain.model.operations.ProductCommandAction
import ai.saniou.thread.domain.model.operations.SourceHealth
import ai.saniou.thread.domain.model.operations.SourceOperationalState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BuildProductCommandsUseCaseTest {
    @Test
    fun connectorStateAndCapabilitiesControlAvailableActions() {
        val commands = BuildProductCommandsUseCase()(
            OperationsSnapshot(
                sources = listOf(
                    SourceHealth(
                        id = "auth-forum",
                        name = "Auth Forum",
                        kind = ContentSourceKind.FORUM,
                        state = SourceOperationalState.AUTHENTICATION_REQUIRED,
                        enabled = true,
                        primaryItemCount = 0,
                        capabilities = setOf("登录", "回复"),
                    ),
                    SourceHealth(
                        id = "disabled-forum",
                        name = "Disabled Forum",
                        kind = ContentSourceKind.FORUM,
                        state = SourceOperationalState.DISABLED,
                        enabled = false,
                        primaryItemCount = 0,
                        capabilities = setOf("登录"),
                    ),
                    SourceHealth(
                        id = "reader",
                        name = "Reader",
                        kind = ContentSourceKind.READER,
                        state = SourceOperationalState.READY,
                        enabled = true,
                        primaryItemCount = 0,
                    ),
                )
            )
        )

        assertTrue(commands.any { it.sourceId == "auth-forum" && it.action == ProductCommandAction.OPEN_SOURCE_LOGIN })
        assertTrue(commands.any { it.sourceId == "disabled-forum" && it.enabledValue == true })
        assertFalse(commands.any { it.sourceId == "disabled-forum" && it.action == ProductCommandAction.REFRESH_SOURCE })
        assertTrue(commands.any { it.action == ProductCommandAction.REFRESH_ALL_READERS })
        assertTrue(commands.any { it.action == ProductCommandAction.EXPORT_DIAGNOSTIC })
    }
}
