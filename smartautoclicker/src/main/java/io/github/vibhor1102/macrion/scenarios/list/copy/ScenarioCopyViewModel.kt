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
package io.github.vibhor1102.macrion.scenarios.list.copy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import io.github.vibhor1102.macrion.core.base.di.Dispatcher
import io.github.vibhor1102.macrion.core.base.di.HiltCoroutineDispatchers.Main
import io.github.vibhor1102.macrion.core.domain.IRepository
import io.github.vibhor1102.macrion.core.dumb.domain.DumbRepository

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScenarioCopyViewModel @Inject constructor(
    @param:Dispatcher(Main) private val mainDispatcher: CoroutineDispatcher,
    private val smartRepository: IRepository,
    private val dumbRepository: DumbRepository,
) : ViewModel() {

    private val _copyName: MutableStateFlow<String?> = MutableStateFlow(null)
    val copyName: StateFlow<String?> = _copyName.asStateFlow()
    val copyNameError: Flow<Boolean> = _copyName
        .map { it.isNullOrEmpty() }

    fun initializeCopyName(name: String) {
        if (_copyName.value == null) _copyName.value = name
    }

    fun setCopyName(name: String) {
        _copyName.value = name
    }

    fun reset() {
        _copyName.value = null
    }

    fun copyScenario(scenarioId: Long, isSmart: Boolean, onCompleted: () -> Unit) {
        val name = _copyName.value
        if (name.isNullOrEmpty()) return

        if (isSmart) {
            smartRepository.addScenarioCopy(scenarioId, name) { onSuccess ->
                viewModelScope.launch(mainDispatcher) {
                    onCompleted()
                }
            }

            return
        }

        dumbRepository.addDumbScenarioCopy(scenarioId, name) {
            viewModelScope.launch(mainDispatcher) {
                onCompleted()
            }
        }
    }
}
