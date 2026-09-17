/*
 * Copyright (C) 2024 Kevin Buzeau
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
package io.github.vibhor1102.macrion.core.domain.model.scenario

import android.os.Build

import androidx.test.ext.junit.runners.AndroidJUnit4

import io.github.vibhor1102.macrion.core.database.entity.CompleteEventEntity
import io.github.vibhor1102.macrion.core.database.entity.CompleteScenario
import io.github.vibhor1102.macrion.core.database.entity.ScenarioWithEvents
import io.github.vibhor1102.macrion.core.domain.model.event.EventTestsData

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ScenarioMapperTests {

    @Test
    fun toEntity() {
        assertEquals(
            ScenarioTestsData.getNewScenarioEntity(),
            ScenarioTestsData.getNewScenario().toEntity()
        )
    }

    @Test
    fun toDomain_from_ScenarioWithEvents() {
        val scenarioWithEvents = ScenarioWithEvents(
            ScenarioTestsData.getNewScenarioEntity(),
            listOf(EventTestsData.getNewTriggerEventEntity(scenarioId = ScenarioTestsData.SCENARIO_ID)),
            stats = null,
        ).toDomain()
        val expectedScenario = ScenarioTestsData.getNewScenario(
            ScenarioTestsData.SCENARIO_ID,
            ScenarioTestsData.SCENARIO_NAME,
            eventCount = 1,
            stats = ScenarioTestsData.defaultStats(),
        )

        assertEquals(expectedScenario, scenarioWithEvents)
    }

    @Test
    fun toDomain_from_CompleteScenario() {
        val (scenario, events) = CompleteScenario(
            scenario = ScenarioTestsData.getNewScenarioEntity(),
            events = listOf(
                CompleteEventEntity(
                    event = EventTestsData.getNewTriggerEventEntity(scenarioId = ScenarioTestsData.SCENARIO_ID),
                    actions = emptyList(),
                    conditions = emptyList(),
                ),
            ),
            counters = emptyList(),
        ).toDomain()
        val expectedScenario = ScenarioTestsData.getNewScenario(ScenarioTestsData.SCENARIO_ID, ScenarioTestsData.SCENARIO_NAME)

        assertEquals(expectedScenario, scenario)
    }
    @Test
    fun folderPositionsSurviveDatabaseAndCopyMapping() {
        val folders = listOf(ScenarioFolder("Before", 0), ScenarioFolder("Empty", 1), ScenarioFolder("After", 1))
        val original = ScenarioTestsData.getNewScenario().copy(folders = folders)
        val entity = original.toEntity()
        assertEquals(folders, ScenarioWithEvents(entity, emptyList(), null).toDomain().folders)
        assertEquals(folders, CompleteScenario(entity, emptyList(), emptyList()).toDomain(cleanIds = true).first.folders)
    }

}