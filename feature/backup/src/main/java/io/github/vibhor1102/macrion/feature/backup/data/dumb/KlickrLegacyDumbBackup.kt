/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.backup.data.dumb

import io.github.vibhor1102.macrion.core.dumb.data.database.DumbActionType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Exact legacy wire fields; Room column names and native additions must not define interchange. */
@Serializable
internal data class KlickrLegacyDumbBackup(
    val version: Int,
    val screenWidth: Int,
    val screenHeight: Int,
    val dumbScenario: KlickrLegacyDumbScenario,
)

@Serializable
internal data class KlickrLegacyDumbScenario(
    val scenario: KlickrLegacyDumbScenarioInfo,
    val dumbActions: List<KlickrLegacyDumbAction>,
)

@Serializable
internal data class KlickrLegacyDumbScenarioInfo(
    val id: Long,
    val name: String,
    val repeatCount: Int,
    val isRepeatInfinite: Boolean,
    val maxDurationMin: Int,
    val isDurationInfinite: Boolean,
    val randomize: Boolean,
)

@Serializable
internal data class KlickrLegacyDumbAction(
    val id: Long,
    @SerialName("dumb_scenario_id") val scenarioId: Long,
    val name: String,
    val type: String,
    val priority: Int,
    val repeatCount: Int? = null,
    val isRepeatInfinite: Boolean? = null,
    val repeatDelay: Long? = null,
    val pressDuration: Long? = null,
    val x: Int? = null,
    val y: Int? = null,
    val swipeDuration: Long? = null,
    val fromX: Int? = null,
    val fromY: Int? = null,
    val toX: Int? = null,
    val toY: Int? = null,
    val pauseDuration: Long? = null,
)

internal fun DumbScenarioBackup.toKlickrLegacyBackup(): KlickrLegacyDumbBackup {
    val info = dumbScenario.scenario
    return KlickrLegacyDumbBackup(
        version = 1,
        screenWidth = screenWidth,
        screenHeight = screenHeight,
        dumbScenario = KlickrLegacyDumbScenario(
            scenario = KlickrLegacyDumbScenarioInfo(
                info.id, info.name, info.repeatCount, info.isRepeatInfinite,
                info.maxDurationMin, info.isDurationInfinite, info.randomize,
            ),
            dumbActions = dumbScenario.dumbActions.map { action ->
                require(action.type in setOf(DumbActionType.CLICK, DumbActionType.SWIPE, DumbActionType.PAUSE))
                require(action.swipePath == null && action.waitBeforeMs == null && action.waitAfterMs == null)
                KlickrLegacyDumbAction(
                    id = action.id, scenarioId = action.dumbScenarioId, name = action.name,
                    type = action.type.name, priority = action.priority,
                    repeatCount = action.repeatCount, isRepeatInfinite = action.isRepeatInfinite,
                    repeatDelay = action.repeatDelay, pressDuration = action.pressDuration,
                    x = action.x, y = action.y, swipeDuration = action.swipeDuration,
                    fromX = action.fromX, fromY = action.fromY, toX = action.toX, toY = action.toY,
                    pauseDuration = action.pauseDuration,
                )
            },
        ),
    )
}
