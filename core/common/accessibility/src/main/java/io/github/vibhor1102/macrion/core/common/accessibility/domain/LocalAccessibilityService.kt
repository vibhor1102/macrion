/*
 * Copyright (C) 2026 Kevin Buzeau
 * Copyright (C) 2026 Vibhor Goel
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
package io.github.vibhor1102.macrion.core.common.accessibility.domain

import android.content.Intent
import io.github.vibhor1102.macrion.core.domain.model.scenario.Scenario
import io.github.vibhor1102.macrion.core.dumb.domain.model.DumbScenario

interface LocalAccessibilityService {

    fun isScenarioRunning(): Boolean
    fun isSmartScreenRecordActive(): Boolean
    fun getSmartScenarioId(): Long?
    fun getDumbScenarioId(): Long?
    fun launchDumbScenario(dumbScenario: DumbScenario): Boolean
    fun launchSmartScenario(resultCode: Int, data: Intent, scenario: Scenario): Boolean
    fun replaceDumbScenario(dumbScenario: DumbScenario)
    fun replaceSmartScenario(resultCode: Int, data: Intent, scenario: Scenario)
    fun replaceSmartScenarioWithCurrentProjection(scenario: Scenario)
    fun runCurrentScenario()
    fun stopScenario()
    fun shutdownForAccessibilityLoss()
    fun release()

}
