/*
 * Copyright (C) 2023 Kevin Buzeau
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
package io.github.vibhor1102.macrion.feature.smart.config.utils

/** Check if this duration value is valid for an action. */
fun Long?.isValidDuration(): Boolean = this != null && this > 0L

const val ALPHA_DISABLED_ITEM = 0.5f
const val ALPHA_DISABLED_ITEM_INT = 127
const val ALPHA_ENABLED_ITEM = 1f
const val ALPHA_ENABLED_ITEM_INT = 255
