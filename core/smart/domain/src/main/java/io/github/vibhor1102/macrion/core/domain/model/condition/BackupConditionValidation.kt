/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.core.domain.model.condition

import io.github.vibhor1102.macrion.core.database.entity.ConditionEntity

fun ConditionEntity.isUsableInBackup(): Boolean =
    runCatching { toDomain(cleanIds = true).isComplete() }.getOrDefault(false)
