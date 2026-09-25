package io.github.vibhor1102.macrion.feature.smart.config.ui.common.model.action

import android.content.Context
import android.graphics.Rect
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vibhor1102.macrion.core.base.identifier.Identifier
import io.github.vibhor1102.macrion.core.domain.model.AND
import io.github.vibhor1102.macrion.core.domain.model.OR
import io.github.vibhor1102.macrion.core.domain.model.action.Click
import io.github.vibhor1102.macrion.core.domain.model.action.SplitAction
import io.github.vibhor1102.macrion.core.domain.model.action.isValidForEvent
import io.github.vibhor1102.macrion.core.domain.model.condition.ScreenCondition
import io.github.vibhor1102.macrion.core.domain.model.event.ScreenEvent
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [29])
class MultiTouchSetupStatusTest {
    @Test fun cardCountsBothChildrenAfterOperatorChangesToAnd() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val eventId = Identifier(databaseId = 1)
        val children = (2L..3L).map { id ->
            Click(
                id = Identifier(databaseId = id), eventId = eventId, name = "Click", priority = id.toInt(),
                pressDuration = 200, positionType = Click.PositionType.ON_DETECTED_CONDITION,
            )
        }
        val split = SplitAction(
            id = Identifier(databaseId = 4), eventId = eventId, name = "Multi-touch",
            priority = 0, subActions = children,
        )
        val condition = ScreenCondition.Color(
            id = Identifier(databaseId = 5), eventId = eventId, name = "Target", threshold = 10,
            shouldBeDetected = true, priority = 0, color = 0, detectionArea = Rect(0, 0, 1, 1),
        )
        val orEvent = ScreenEvent(
            id = eventId, scenarioId = Identifier(databaseId = 6), name = "Event",
            conditionOperator = OR, actions = listOf(split), conditions = listOf(condition),
            priority = 0, keepDetecting = false, cooldownMs = 0,
        )
        val andEvent = orEvent.copy(conditionOperator = AND)

        assertEquals(0, split.toUiAction(context, orEvent).subUiActions.count { it.haveError })
        val card = split.toUiAction(context, andEvent, inError = !split.isValidForEvent(andEvent))
        assertEquals(2, card.subUiActions.count { it.haveError })
    }
}
