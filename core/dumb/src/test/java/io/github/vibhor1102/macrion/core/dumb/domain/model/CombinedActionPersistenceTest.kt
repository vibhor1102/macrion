package io.github.vibhor1102.macrion.core.dumb.domain.model

import android.graphics.Point
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vibhor1102.macrion.core.base.identifier.Identifier
import io.github.vibhor1102.macrion.core.dumb.data.database.DumbActionEntity
import io.github.vibhor1102.macrion.core.dumb.data.database.DumbActionType
import io.github.vibhor1102.macrion.core.dumb.data.database.DumbActionWithSubActions
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [29])
class CombinedActionPersistenceTest {
    private fun click() = DumbAction.DumbClick(Identifier(databaseId = 1), Identifier(databaseId = 2),
        "Custom finger", 0, 3, false, 150, Point(0, 0), 250, 100, 400)

    @Test fun originIsValidButUnconfiguredPositionIsNot() {
        assertTrue(click().isValid())
        assertFalse(click().copy(position = Point(-1, -1)).isValid())
    }
    @Test fun childMetadataSurvivesReloadForUnsplit() {
        val original = click()
        assertEquals(original, original.toSplitItemEntity(10, 0).toDomain(false, 2))
    }
    @Test fun groupsRejectUnsupportedOrExcessiveChildren() {
        val parent = DumbAction.DumbSplitAction(Identifier(databaseId = 10), Identifier(databaseId = 2),
            "Zoom", subActions = listOf(click(), click()))
        assertTrue(parent.isValid())
        assertFalse(parent.copy(subActions = List(11) { click() }).isValid())
        assertFalse(parent.copy(subActions = listOf(parent, click())).isValid())
    }

    @Test fun savedMultiTouchDefaultsUseCurrentLabel() {
        listOf("Simultaneous click/swipe", "Simultaneous Touch").forEach { oldName ->
            val saved = DumbActionEntity(10, 2, name = oldName, type = DumbActionType.SPLIT_ACTION)
            assertEquals("Multi-touch", (saved.toDomain() as DumbAction.DumbSplitAction).name)
            assertEquals("Multi-touch", (DumbActionWithSubActions(saved).toDomain() as DumbAction.DumbSplitAction).name)
            assertEquals(oldName, saved.name)
        }
        val custom = DumbActionEntity(10, 2, name = "Custom gesture", type = DumbActionType.SPLIT_ACTION)
        assertEquals("Custom gesture", (DumbActionWithSubActions(custom).toDomain() as DumbAction.DumbSplitAction).name)
    }
}
