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
package io.github.vibhor1102.macrion.core.common.overlays.manager

import androidx.lifecycle.Lifecycle
import io.github.vibhor1102.macrion.core.common.overlays.base.Overlay
import io.github.vibhor1102.macrion.core.common.overlays.menu.OverlayMenu

internal class LifecycleStatesRegistry {

    private val overlaysLifecycleState = mutableMapOf<Overlay, Lifecycle.State>()

    fun saveStates(overlays: List<Overlay>) {
        overlaysLifecycleState.clear()
        overlays.forEach { overlay ->
            overlaysLifecycleState[overlay] = (overlay as? OverlayMenu)?.stateToRestoreAfterHide
                ?: overlay.lifecycle.currentState
        }
    }

    fun saveNewOverlayState(overlay: Overlay, state: Lifecycle.State) {
        overlaysLifecycleState[overlay] = state
    }

    fun restoreStates(): Map<Overlay, Lifecycle.State> {
        val states = overlaysLifecycleState.toMap()
        overlaysLifecycleState.clear()
        return states
    }

    fun clearStates() {
        overlaysLifecycleState.clear()
    }

    fun haveStates(): Boolean = overlaysLifecycleState.isNotEmpty()
}