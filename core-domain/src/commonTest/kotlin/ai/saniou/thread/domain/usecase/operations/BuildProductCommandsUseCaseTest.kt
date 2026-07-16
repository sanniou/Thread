package ai.saniou.thread.domain.usecase.operations

import ai.saniou.thread.domain.model.operations.ContentSourceKind
import ai.saniou.thread.domain.model.operations.OperationsSnapshot
import ai.saniou.thread.domain.model.operations.ProductCommandAction
import ai.saniou.thread.domain.model.operations.SourceHealth
import ai.saniou.thread.domain.model.operations.SourceOperationalState
import ai.saniou.thread.domain.model.activity.ActivityCenterSnapshot
import ai.saniou.thread.domain.model.activity.IdentityLoginKind
import ai.saniou.thread.domain.model.activity.IdentityValidity
import ai.saniou.thread.domain.model.activity.ProductActionType
import ai.saniou.thread.domain.model.activity.SourceIdentityStatus
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BuildProductCommandsUseCaseTest {
    @Test
    fun connectorStateAndCapabilitiesControlAvailableActions() {
        val commands = BuildProductCommandsUseCase()(
            ActivityCenterSnapshot(
                operations = OperationsSnapshot(
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
                ),
                identities = listOf(
                    SourceIdentityStatus(
                        sourceId = "auth-forum",
                        sourceName = "Auth Forum",
                        validity = IdentityValidity.EXPIRED,
                        loginKind = IdentityLoginKind.API,
                        supportsLogin = true,
                    )
                ),
            )
        )

        assertTrue(commands.any { it.sourceId == "auth-forum" && it.action == ProductCommandAction.OPEN_SOURCE_LOGIN })
        assertTrue(commands.any { it.sourceId == "disabled-forum" && it.enabledValue == true })
        assertFalse(commands.any { it.sourceId == "disabled-forum" && it.request?.type == ProductActionType.REFRESH_SOURCE })
        assertTrue(commands.any { it.request?.type == ProductActionType.REFRESH_ALL_READERS })
        assertTrue(commands.any { it.request?.type == ProductActionType.EXPORT_DIAGNOSTIC })
        assertTrue(commands.any { it.action == ProductCommandAction.OPEN_ACTIVITY_CENTER })
        assertTrue(commands.count { it.request?.type == ProductActionType.EXPORT_READER_SUBSCRIPTIONS } == 2)
    }
}
