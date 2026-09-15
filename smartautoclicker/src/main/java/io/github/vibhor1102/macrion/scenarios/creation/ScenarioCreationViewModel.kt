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
package io.github.vibhor1102.macrion.scenarios.creation

import android.content.Context

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import io.github.vibhor1102.macrion.R
import io.github.vibhor1102.macrion.core.base.identifier.DATABASE_ID_INSERTION
import io.github.vibhor1102.macrion.core.base.identifier.Identifier
import io.github.vibhor1102.macrion.core.display.config.DisplayConfigManager
import io.github.vibhor1102.macrion.core.domain.IRepository
import io.github.vibhor1102.macrion.core.domain.model.scenario.Scenario
import io.github.vibhor1102.macrion.core.dumb.domain.IDumbRepository
import io.github.vibhor1102.macrion.core.dumb.domain.model.DumbScenario
import io.github.vibhor1102.macrion.core.processing.domain.DETECTION_QUALITY_MIN
import io.github.vibhor1102.macrion.feature.revenue.IRevenueRepository
import io.github.vibhor1102.macrion.feature.revenue.UserBillingState

import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.floor
import kotlin.math.max

@HiltViewModel
class ScenarioCreationViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    revenueRepository: IRevenueRepository,
    private val smartRepository: IRepository,
    private val dumbRepository: IDumbRepository,
    private val displayConfigManager: DisplayConfigManager,
) : ViewModel() {

    fun reset() {
        _name.value = context.getString(R.string.default_scenario_name)
        _selectedType.value = ScenarioTypeSelection.SMART
        _creationState.value = CreationState.CONFIGURING
    }

    private val _name: MutableStateFlow<String?> =
        MutableStateFlow(context.getString(R.string.default_scenario_name))
    val name: Flow<String> = _name
        .map { it ?: "" }
    val nameError: Flow<Boolean> = _name
        .map { it.isNullOrBlank() }
    val showPaidLimitationWarning: Flow<Boolean> = revenueRepository.userBillingState
        .map { it != UserBillingState.PURCHASED }

    private val _selectedType: MutableStateFlow<ScenarioTypeSelection> =
        MutableStateFlow(ScenarioTypeSelection.SMART)
    val scenarioTypeSelectionState: Flow<ScenarioTypeSelectionState> =
        combine(_selectedType, revenueRepository.userBillingState) { selectedType, billingState ->
            ScenarioTypeSelectionState(
                dumbItem = ScenarioTypeItem.Dumb,
                smartItem = ScenarioTypeItem.Smart,
                selectedItem = selectedType,
            )
        }

    private val canBeCreated: Flow<Boolean> = _name.map { name -> !name.isNullOrBlank() }
    private val _creationState: MutableStateFlow<CreationState> =
        MutableStateFlow(CreationState.CONFIGURING)
    val creationState: Flow<CreationState> = _creationState.combine(canBeCreated) { state, valid ->
        if (state == CreationState.CONFIGURING && !valid) CreationState.CONFIGURING_INVALID
        else state
    }

    fun setName(newName: String?) {
        _name.value = newName
    }

    fun setSelectedType(type: ScenarioTypeSelection) {
        _selectedType.value = type
    }

    fun createScenario() {
        if (isInvalidForCreation() || _creationState.value != CreationState.CONFIGURING) return

        _creationState.value = CreationState.CREATING
        viewModelScope.launch(Dispatchers.IO) {
            when (_selectedType.value) {
                ScenarioTypeSelection.DUMB -> createDumbScenario()
                ScenarioTypeSelection.SMART -> createSmartScenario()
            }
            _creationState.value = CreationState.SAVED
        }
    }

    private suspend fun createDumbScenario() {
        dumbRepository.addDumbScenario(
            DumbScenario(
                id = Identifier(databaseId = DATABASE_ID_INSERTION, tempId = 0L),
                name = _name.value!!.trim(),
                dumbActions = emptyList(),
                repeatCount = 1,
                isRepeatInfinite = false,
                maxDurationMin = 1,
                isDurationInfinite = true,
                randomize = false,
            )
        )
    }

    private suspend fun createSmartScenario() {
        smartRepository.addScenario(
            Scenario(
                id = Identifier(databaseId = DATABASE_ID_INSERTION, tempId = 0L),
                name = _name.value!!.trim(),
                detectionQuality = getDefaultDetectionQuality(),
                randomize = false,
            )
        )
    }

    private fun getDefaultDetectionQuality(): Int {
        val displaySize = displayConfigManager.displayConfig.sizePx
        val biggestScreenSideSize: Int = max(displaySize.x, displaySize.y)

        return max(
            DETECTION_QUALITY_MIN.toInt(),
            floor(biggestScreenSideSize / DEFAULT_DETECTION_QUALITY_RATIO).toInt(),
        )
    }

    private fun isInvalidForCreation(): Boolean = _name.value.isNullOrBlank()
}


data class ScenarioTypeSelectionState(
    val dumbItem: ScenarioTypeItem.Dumb,
    val smartItem: ScenarioTypeItem.Smart,
    val selectedItem: ScenarioTypeSelection,
)

sealed class ScenarioTypeItem(val titleRes: Int, val iconRes: Int, val descriptionText: Int) {

    data object Dumb: ScenarioTypeItem(
        titleRes = R.string.item_title_dumb_scenario,
        iconRes = R.drawable.ic_dumb,
        descriptionText = R.string.item_desc_dumb_scenario,
    )

    data object Smart: ScenarioTypeItem(
        titleRes = R.string.item_title_smart_scenario,
        iconRes = R.drawable.ic_smart,
        descriptionText = R.string.item_desc_smart_scenario,
    )
}
enum class ScenarioTypeSelection {
    DUMB,
    SMART,
}

enum class CreationState {
    CONFIGURING_INVALID,
    CONFIGURING,
    CREATING,
    SAVED,
}

private const val DEFAULT_DETECTION_QUALITY_RATIO = 2.05
