package io.github.vibhor1102.macrion.core.domain.model.action

import android.graphics.Point
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vibhor1102.macrion.core.base.identifier.Identifier
import io.github.vibhor1102.macrion.core.database.entity.CompleteActionEntity
import io.github.vibhor1102.macrion.core.domain.model.action.mapper.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [29])
class CombinedActionPersistenceTest {
    @Test fun namesConditionReferencesOffsetsAndDelaysSurviveReload() {
        val eventId = Identifier(databaseId = 5)
        val click = Click(Identifier(databaseId = 2), eventId, "Follow image", 0, 100,
            Click.PositionType.ON_DETECTED_CONDITION, clickOnConditionId = Identifier(databaseId = 9),
            clickOffset = Point(-10, 20), waitBeforeMs = 250, waitAfterMs = 450)
        val swipe = Swipe(Identifier(databaseId = 3), eventId, "Other finger", 1, 500,
            Point(20, 20), Point(30, 30), 100, 20)
        val split = SplitAction(Identifier(databaseId = 1), eventId, "Zoom", 0, listOf(click, swipe))
        val entity = CompleteActionEntity(action = split.toEntity(),
            splitItems = split.subActions.mapIndexed { index, action -> action.toSplitItemEntity(1, index) })
        assertEquals(split, entity.toDomain())
        assertTrue(split.isComplete())
        assertFalse(split.copy(subActions = listOf(split, swipe)).isComplete())
    }
}
