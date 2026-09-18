/*
 * Copyright (C) 2026 Vibhor Goel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.feature.smart.config.ui.action.split

import androidx.annotation.DrawableRes
import io.github.vibhor1102.macrion.core.domain.model.action.Action

data class SubActionItemUiState(
    val index: Int,
    val name: String,
    val details: String,
    @DrawableRes val icon: Int,
    val isComplete: Boolean,
    val action: Action,
)

data class SplitActionUiState(
    val name: String,
    val nameError: Boolean,
    val canBeSaved: Boolean,
    val hasUnsavedModifications: Boolean,
    val subActions: List<SubActionItemUiState>,
    val durationMs: Long,
    val canDeleteSubAction: Boolean,
    val canUnsplit: Boolean,
)
