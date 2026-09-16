/*
 * Copyright (C) 2024 Kevin Buzeau
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
package io.github.vibhor1102.macrion.core.database

import androidx.room.AutoMigration
import androidx.room.Database

import io.github.vibhor1102.macrion.core.database.entity.ActionEntity
import io.github.vibhor1102.macrion.core.database.entity.ConditionEntity
import io.github.vibhor1102.macrion.core.database.entity.CountersEntity
import io.github.vibhor1102.macrion.core.database.entity.EventEntity
import io.github.vibhor1102.macrion.core.database.entity.EventToggleEntity
import io.github.vibhor1102.macrion.core.database.entity.IntentExtraEntity
import io.github.vibhor1102.macrion.core.database.entity.ScenarioEntity
import io.github.vibhor1102.macrion.core.database.entity.ScenarioStatsEntity
import io.github.vibhor1102.macrion.core.database.migrations.*

import javax.inject.Singleton

@Singleton
@Database(
    entities = [
        ActionEntity::class,
        EventEntity::class,
        ScenarioEntity::class,
        ConditionEntity::class,
        IntentExtraEntity::class,
        EventToggleEntity::class,
        ScenarioStatsEntity::class,
        CountersEntity::class,
    ],
    version = DATABASE_VERSION,
    exportSchema = true,
    autoMigrations = [
        AutoMigration (from = 7, to = 8),
        AutoMigration (from = 8, to = 9, spec = Migration8to9::class),
        AutoMigration (from = 11, to = 12),
        AutoMigration (from = 13, to = 14),
        AutoMigration (from = 14, to = 15),
        AutoMigration (from = 15, to = 16),
        AutoMigration (from = 16, to = 17),
        AutoMigration (from = 17, to = 18),
        AutoMigration (from = 18, to = 19),
        AutoMigration (from = 20, to = 21),
        AutoMigration (from = 22, to = 23),
        AutoMigration (from = 24, to = 25),
        AutoMigration (from = 26, to = 27),
    ]
)
abstract class ClickDatabase : ScenarioDatabase()
