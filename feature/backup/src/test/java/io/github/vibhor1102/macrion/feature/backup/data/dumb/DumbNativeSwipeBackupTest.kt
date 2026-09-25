package io.github.vibhor1102.macrion.feature.backup.data.dumb

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vibhor1102.macrion.core.base.gesture.SwipeNode
import io.github.vibhor1102.macrion.core.base.gesture.SwipePath
import io.github.vibhor1102.macrion.core.base.gesture.SwipePoint
import io.github.vibhor1102.macrion.core.dumb.data.database.DUMB_DATABASE_VERSION
import io.github.vibhor1102.macrion.core.dumb.data.database.DumbActionEntity
import io.github.vibhor1102.macrion.core.dumb.data.database.DumbActionType
import io.github.vibhor1102.macrion.core.dumb.data.database.DumbActionWithSubActions
import io.github.vibhor1102.macrion.core.dumb.data.database.DumbScenarioEntity
import io.github.vibhor1102.macrion.core.dumb.data.database.DumbScenarioWithActions
import io.github.vibhor1102.macrion.core.dumb.data.database.DumbSplitActionItemEntity
import io.github.vibhor1102.macrion.feature.backup.data.base.BackupArchiveFormat
import io.github.vibhor1102.macrion.feature.backup.data.base.MACRION_FORMAT_NAME
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [29])
class DumbNativeSwipeBackupTest {
    @Test fun nativeBackupRetainsCurvedMultiTouchChild() {
        val path = SwipePath(listOf(
            SwipeNode(SwipePoint(10f, 20f)),
            SwipeNode(SwipePoint(30f, 40f)),
            SwipeNode(SwipePoint(50f, 20f)),
        ))
        val parent = DumbActionEntity(
            id = 2, dumbScenarioId = 1, name = "Multi-touch", type = DumbActionType.SPLIT_ACTION,
        )
        val child = DumbSplitActionItemEntity(
            id = 3, actionId = 2, type = DumbActionType.SWIPE,
            fromX = 10, fromY = 20, toX = 50, toY = 20, duration = 500, swipePath = path,
        )
        val source = DumbScenarioBackup(
            format = MACRION_FORMAT_NAME,
            version = DUMB_DATABASE_VERSION,
            screenWidth = 1080,
            screenHeight = 2400,
            dumbScenario = DumbScenarioWithActions(
                scenario = DumbScenarioEntity(1, "Scenario", 1, false, 1, true, false),
                dumbActions = listOf(parent),
                stats = null,
                dumbActionsWithSubActions = listOf(DumbActionWithSubActions(parent, listOf(child))),
            ),
        )
        val bytes = ByteArrayOutputStream().also { DumbScenarioSerializer().serialize(source, it) }.toByteArray()

        val restored = DumbScenarioSerializer().deserialize(
            ByteArrayInputStream(bytes), BackupArchiveFormat.MACRION_NATIVE,
        )!!

        assertEquals(path, restored.dumbScenario.dumbActionsWithSubActions.single().splitItems.single().swipePath)
    }
}
