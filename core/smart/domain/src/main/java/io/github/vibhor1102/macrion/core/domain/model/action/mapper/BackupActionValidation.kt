/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.core.domain.model.action.mapper

import io.github.vibhor1102.macrion.core.database.entity.CompleteActionEntity

fun CompleteActionEntity.isUsableInBackup(): Boolean =
    runCatching { toDomain(cleanIds = true).isComplete() }.getOrDefault(false)
