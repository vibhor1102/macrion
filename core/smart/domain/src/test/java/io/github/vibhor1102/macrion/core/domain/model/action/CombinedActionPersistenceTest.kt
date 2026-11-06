package io.github.vibhor1102.macrion.core.domain.model.action

import io.github.vibhor1102.macrion.core.domain.model.action.PositionCounters
import io.github.vibhor1102.macrion.core.domain.model.counter.ConditionCoordinate
import io.github.vibhor1102.macrion.core.domain.model.counter.CounterOperationValue

import android.graphics.Point
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vibhor1102.macrion.core.base.identifier.Identifier
import io.github.vibhor1102.macrion.core.base.gesture.SwipeNode
import io.github.vibhor1102.macrion.core.base.gesture.SwipePath
import io.github.vibhor1102.macrion.core.base.gesture.SwipePoint
import io.github.vibhor1102.macrion.core.database.entity.CompleteActionEntity
import io.github.vibhor1102.macrion.core.domain.model.action.mapper.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [29])
class CombinedActionPersistenceTest {
    @Test fun counterPositionsAndDetectedCoordinateOperandsSurviveReload() {
        val event = Identifier(databaseId = 5)
        val counters = PositionCounters("X", "Y")
        val click = Click(Identifier(databaseId = 2), event, "Remembered", 0, 100,
            Click.PositionType.USER_SELECTED, positionCounters = counters)
        val swipe = Swipe(Identifier(databaseId = 3), event, "Move", 1, 500,
            to = Point(50, 60), fromCounters = counters)
        assertEquals(click, CompleteActionEntity(click.toEntity()).toDomain())
        assertEquals(swipe, CompleteActionEntity(swipe.toEntity()).toDomain())
        val group = SplitAction(Identifier(databaseId = 1), event, "Group", 0, listOf(click, swipe))
        assertEquals(group, CompleteActionEntity(group.toEntity(), splitItems = group.subActions.mapIndexed {
            index, action -> action.toSplitItemEntity(1, index)
        }).toDomain())
        val change = ChangeCounter(Identifier(databaseId = 4), event, "Remember edge", 0, "X",
            ChangeCounter.OperationType.SET,
            CounterOperationValue.ConditionPosition(
                Identifier(databaseId = 9), ConditionCoordinate.RIGHT,
            ))
        assertEquals(change, CompleteActionEntity(change.toEntity()).toDomain())
        val copied = CompleteActionEntity(change.toEntity()).toDomain(cleanIds = true) as ChangeCounter
        assertEquals(9L, (copied.operationValue as CounterOperationValue.ConditionPosition).conditionId?.tempId)
        assertEquals(listOf("X", "Y", "X", "Y"), group.positionCounterNames())
        val renamed = group.renamePositionCounter("X", "Saved X")
        assertFalse("X" in renamed.positionCounterNames())
        assertEquals(listOf("X", "Y", "X", "Y"), group.positionCounterNames())
    }

    @Test fun recordedFractionalCurveCanBeSaved() {
        val path = SwipePath(listOf(
            SwipeNode(SwipePoint(12.8f, 20.9f)),
            SwipeNode(SwipePoint(45.2f, 90.4f)),
            SwipeNode(SwipePoint(90.7f, 25.6f)),
        ))
        val swipe = Swipe(
            id = Identifier(databaseId = 10), eventId = Identifier(databaseId = 5),
            name = "Curve", priority = 0, swipeDuration = 500,
            from = Point(12, 20), to = Point(90, 25), path = path,
        )

        assertTrue(swipe.isComplete())
        assertFalse(swipe.copy(to = Point(91, 25)).isComplete())
    }

    @Test fun namesConditionReferencesOffsetsAndDelaysSurviveReload() {
        val eventId = Identifier(databaseId = 5)
        val click = Click(Identifier(databaseId = 2), eventId, "Follow image", 0, 100,
            Click.PositionType.ON_DETECTED_CONDITION, clickOnConditionId = Identifier(databaseId = 9),
            clickOffset = Point(-10, 20), waitBeforeMs = 250, waitAfterMs = 450)
        val swipe = Swipe(Identifier(databaseId = 3), eventId, "Other finger", 1, 500,
            Point(20, 20), Point(30, 30), waitBeforeMs = 100, waitAfterMs = 20)
        val split = SplitAction(Identifier(databaseId = 1), eventId, "Zoom", 0, listOf(click, swipe))
        val entity = CompleteActionEntity(action = split.toEntity(),
            splitItems = split.subActions.mapIndexed { index, action -> action.toSplitItemEntity(1, index) })
        assertEquals(split, entity.toDomain())
        assertTrue(split.isComplete())
        assertFalse(split.copy(subActions = listOf(split, swipe)).isComplete())
    }
}
