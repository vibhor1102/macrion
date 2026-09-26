package io.github.vibhor1102.macrion.core.dumb.domain.model

import android.graphics.Point
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vibhor1102.macrion.core.base.identifier.Identifier
import io.github.vibhor1102.macrion.core.base.gesture.SwipeNode
import io.github.vibhor1102.macrion.core.base.gesture.SwipePath
import io.github.vibhor1102.macrion.core.base.gesture.SwipePoint
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [29])
class CombinedActionPersistenceTest {
    @Test fun recordedFractionalCurveCanBeSaved() {
        val path = SwipePath(listOf(
            SwipeNode(SwipePoint(12.8f, 20.9f)),
            SwipeNode(SwipePoint(45.2f, 90.4f)),
            SwipeNode(SwipePoint(90.7f, 25.6f)),
        ))
        val swipe = DumbAction.DumbSwipe(
            id = Identifier(databaseId = 10), scenarioId = Identifier(databaseId = 2),
            name = "Curve", repeatCount = 1, isRepeatInfinite = false, repeatDelayMs = 0,
            fromPosition = Point(12, 20), toPosition = Point(90, 25),
            path = path, swipeDurationMs = 500,
        )

        assertTrue(swipe.isValid())
        assertFalse(swipe.copy(toPosition = Point(91, 25)).isValid())
    }

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
}
