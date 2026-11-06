/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.core.dumb.domain.model

import io.github.vibhor1102.macrion.core.dumb.data.database.DumbActionWithSubActions

fun DumbActionWithSubActions.isUsableInBackup(): Boolean =
    runCatching { toDomain(asDomain = true).isValid() }.getOrDefault(false)
