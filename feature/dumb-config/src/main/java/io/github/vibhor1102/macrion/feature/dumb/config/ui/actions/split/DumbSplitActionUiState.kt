/*
 * Copyright (C) 2026 Vibhor Goel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.feature.dumb.config.ui.actions.split

import androidx.annotation.DrawableRes
import io.github.vibhor1102.macrion.core.dumb.domain.model.DumbAction

data class DumbSubActionItemUiState(
    val index: Int,
    val name: String,
    val details: String,
    @DrawableRes val icon: Int,
    val isComplete: Boolean,
    val action: DumbAction,
)

data class DumbSplitActionUiState(
    val name: String,
    val nameError: Boolean,
    val repeatCount: String,
    val repeatCountError: Boolean,
    val repeatDelay: String,
    val repeatDelayError: Boolean,
    val isRepeatInfinite: Boolean,
    val waitBefore: String,
    val waitAfter: String,
    val canBeSaved: Boolean,
    val subActions: List<DumbSubActionItemUiState>,
    val durationMs: Long,
    val canDeleteSubAction: Boolean,
    val canUnsplit: Boolean,
)
