/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.vibhor1102.macrion.feature.backup.data.smart

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vibhor1102.macrion.core.base.gesture.SwipeNode
import io.github.vibhor1102.macrion.core.base.gesture.SwipePath
import io.github.vibhor1102.macrion.core.base.gesture.SwipePoint
import io.github.vibhor1102.macrion.core.database.DATABASE_VERSION
import io.github.vibhor1102.macrion.core.database.entity.ActionEntity
import io.github.vibhor1102.macrion.core.database.entity.ActionType
import io.github.vibhor1102.macrion.core.database.entity.CompleteActionEntity
import io.github.vibhor1102.macrion.core.database.entity.CompleteEventEntity
import io.github.vibhor1102.macrion.core.database.entity.CompleteScenario
import io.github.vibhor1102.macrion.core.database.entity.ConditionEntity
import io.github.vibhor1102.macrion.core.database.entity.ConditionType
import io.github.vibhor1102.macrion.core.database.entity.CountersEntity
import io.github.vibhor1102.macrion.core.database.entity.EventEntity
import io.github.vibhor1102.macrion.core.database.entity.EventType
import io.github.vibhor1102.macrion.core.database.entity.ScenarioEntity
import io.github.vibhor1102.macrion.feature.backup.data.base.BackupArchiveFormat
import io.github.vibhor1102.macrion.feature.backup.data.base.MACRION_FORMAT_NAME
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class SmartBackupDataSourceTests {

    @Test
    fun nativeBackupRetainsCurvedSwipeGeometry() {
        val path = SwipePath(listOf(
            SwipeNode(SwipePoint(10f, 20f), controlOut = SwipePoint(20f, 20f)),
            SwipeNode(SwipePoint(50f, 60f), controlIn = SwipePoint(40f, 60f)),
        ))
        val source = ScenarioBackup(
            format = MACRION_FORMAT_NAME,
            version = DATABASE_VERSION,
            screenWidth = 1080,
            screenHeight = 2400,
            scenario = CompleteScenario(
                scenario = ScenarioEntity(1, "Scenario", 600, 0.0, false),
                counters = emptyList(),
                events = listOf(CompleteEventEntity(
                    event = EventEntity(1, 1, "Event", 1, 0, true, EventType.TRIGGER_EVENT),
                    conditions = emptyList(),
                    actions = listOf(CompleteActionEntity(
                        action = ActionEntity(
                            id = 1, eventId = 1, name = "Swipe", type = ActionType.SWIPE,
                            fromX = 10, fromY = 20, toX = 50, toY = 60, swipeDuration = 500,
                            swipePath = path,
                        ),
                        intentExtras = emptyList(), eventsToggle = emptyList(),
                    )),
                )),
            ),
        )
        val bytes = ByteArrayOutputStream().also { ScenarioSerializer().serialize(source, it) }.toByteArray()

        val restored = ScenarioSerializer().deserialize(
            ByteArrayInputStream(bytes), BackupArchiveFormat.MACRION_NATIVE,
        )!!

        assertEquals(path, restored.scenario.events.single().actions.single().action.swipePath)
    }

    @Test
    fun deserialize_normalizesNamesAndCounterReferences_withoutTrimmingPayload() {
        val serializer = ScenarioSerializer()
        val source = ScenarioBackup(
            version = DATABASE_VERSION,
            screenWidth = 1080,
            screenHeight = 2400,
            scenario = CompleteScenario(
                scenario = ScenarioEntity(1, " Scenario ", 600, 0.0, false),
                counters = listOf(
                    CountersEntity("Score", 1, 1.0),
                    CountersEntity(" Score ", 1, 2.0),
                ),
                events = listOf(CompleteEventEntity(
                    event = EventEntity(1, 1, " Event ", 1, 0, true, EventType.TRIGGER_EVENT),
                    conditions = listOf(ConditionEntity(
                        id = 1, eventId = 1, name = " Condition ", type = ConditionType.ON_COUNTER_REACHED,
                        priority = 0, counterName = " Score ", counterOperationCounterName = "Score",
                        numberCounterOperationCounterName = " Score ",
                    )),
                    actions = listOf(
                        CompleteActionEntity(
                        action = ActionEntity(
                            id = 1, eventId = 1, name = " Notify ", type = ActionType.NOTIFICATION,
                            notificationMessageText = "  value={ Score }  ", notificationImportance = 3,
                        ),
                        intentExtras = emptyList(), eventsToggle = emptyList(),
                    ),
                        CompleteActionEntity(
                            action = ActionEntity(
                                id = 2, eventId = 1, name = " Text ", type = ActionType.TEXT,
                                textValue = "first={ Score }, unknown={Missing}, empty={}",
                            ),
                            intentExtras = emptyList(), eventsToggle = emptyList(),
                        ),
                        CompleteActionEntity(
                            action = ActionEntity(
                                id = 3, eventId = 1, name = " Change ", type = ActionType.CHANGE_COUNTER,
                                counterName = " Score ", counterOperationCounterName = "Score",
                            ),
                            intentExtras = emptyList(), eventsToggle = emptyList(),
                        ),
                        CompleteActionEntity(
                            action = ActionEntity(
                                id = 4, eventId = 1, name = " External ", type = ActionType.EXTERNAL_ACTION,
                                externalActionName = " External action ",
                            ),
                            intentExtras = emptyList(), eventsToggle = emptyList(),
                        ),
                    ),
                )),
            ),
        )
        val bytes = ByteArrayOutputStream().also { serializer.serialize(source, it) }.toByteArray()

        val restored = serializer.deserialize(
            ByteArrayInputStream(bytes), BackupArchiveFormat.KLICKR_COMPATIBLE,
        )!!.scenario

        assertEquals("Scenario", restored.scenario.name)
        assertEquals("Event", restored.events.single().event.name)
        assertEquals("Notify", restored.events.single().actions.first().action.name)
        assertEquals(listOf("Score", "Score (2)"), restored.counters.map { it.name }.sorted())
        val restoredEvent = restored.events.single()
        assertEquals("Condition", restoredEvent.conditions.single().name)
        assertEquals("Score (2)", restoredEvent.conditions.single().counterName)
        assertEquals("Score", restoredEvent.conditions.single().counterOperationCounterName)
        assertEquals("Score (2)", restoredEvent.conditions.single().numberCounterOperationCounterName)
        assertEquals("  value={Score (2)}  ", restoredEvent.actions[0].action.notificationMessageText)
        assertEquals("first={Score (2)}, unknown={Missing}, empty={}", restoredEvent.actions[1].action.textValue)
        assertEquals("Score (2)", restoredEvent.actions[2].action.counterName)
        assertEquals("Score", restoredEvent.actions[2].action.counterOperationCounterName)
        assertEquals("External", restoredEvent.actions[3].action.name)
        assertEquals("External action", restoredEvent.actions[3].action.externalActionName)
    }

    @Test
    fun sameScreenSize_sameOrientation_noWarning() {
        assertFalse(hasDifferentScreenSize(1080, 2400, 1080, 2400))
    }

    @Test
    fun sameScreenSize_differentOrientation_noWarning() {
        assertFalse(hasDifferentScreenSize(1080, 2400, 2400, 1080))
    }

    @Test
    fun differentScreenSize_warning() {
        assertTrue(hasDifferentScreenSize(1080, 2400, 1440, 3200))
    }
}
