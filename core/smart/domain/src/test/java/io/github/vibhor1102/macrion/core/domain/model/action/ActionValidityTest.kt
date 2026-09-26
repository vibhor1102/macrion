package io.github.vibhor1102.macrion.core.domain.model.action

import android.graphics.Rect
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vibhor1102.macrion.core.base.identifier.Identifier
import io.github.vibhor1102.macrion.core.domain.model.AND
import io.github.vibhor1102.macrion.core.domain.model.OR
import io.github.vibhor1102.macrion.core.domain.model.condition.ScreenCondition
import io.github.vibhor1102.macrion.core.domain.model.event.ScreenEvent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [29])
class ActionValidityTest {
    private val eventId = Identifier(databaseId = 1)
    private val conditionId = Identifier(databaseId = 2)
    private val condition = ScreenCondition.Color(
        id = conditionId, eventId = eventId, name = "Target", threshold = 10,
        shouldBeDetected = true, priority = 0, color = 0, detectionArea = Rect(0, 0, 1, 1),
    )

    private fun click(id: Long, target: Identifier? = null) = Click(
        id = Identifier(databaseId = id), eventId = eventId, name = "Click", priority = id.toInt(),
        pressDuration = 200, positionType = Click.PositionType.ON_DETECTED_CONDITION,
        clickOnConditionId = target,
    )

    private fun event(operator: Int, action: SplitAction) = ScreenEvent(
        id = eventId, scenarioId = Identifier(databaseId = 3), name = "Event",
        conditionOperator = operator, actions = listOf(action), conditions = listOf(condition),
        priority = 0, keepDetecting = false, cooldownMs = 0,
    )

    @Test fun bothChildrenNeedSetupAfterChangingOrToAnd() {
        val split = SplitAction(
            id = Identifier(databaseId = 4), eventId = eventId, name = "Multi-touch", priority = 0,
            subActions = listOf(click(5), click(6)),
        )
        val orEvent = event(OR, split)
        val andEvent = event(AND, split)

        assertTrue(orEvent.isComplete())
        assertTrue(split.subActions.all { it.isValidMultiTouchChildForEvent(orEvent) })
        assertFalse(andEvent.isComplete())
        assertTrue(split.subActions.none { it.isValidMultiTouchChildForEvent(andEvent) })

        val firstConfigured = split.copy(subActions = listOf(click(5, conditionId), click(6)))
        val partiallyConfiguredEvent = event(AND, firstConfigured)
        assertFalse(partiallyConfiguredEvent.isComplete())
        assertTrue(firstConfigured.subActions[0].isValidMultiTouchChildForEvent(partiallyConfiguredEvent))
        assertFalse(firstConfigured.subActions[1].isValidMultiTouchChildForEvent(partiallyConfiguredEvent))

        val configured = split.copy(subActions = listOf(click(5, conditionId), click(6, conditionId)))
        assertTrue(event(AND, configured).isComplete())
    }
}
