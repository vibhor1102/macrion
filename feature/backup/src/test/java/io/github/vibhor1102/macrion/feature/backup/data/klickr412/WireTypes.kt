/* Copyright (C) 2026 Kevin Buzeau; test wire snapshots adapted by Vibhor Goel. */
package io.github.vibhor1102.macrion.feature.backup.data.klickr412

import kotlinx.serialization.Serializable
import io.github.vibhor1102.macrion.core.dumb.data.database.DumbScenarioEntity
import io.github.vibhor1102.macrion.core.dumb.data.database.DumbScenarioStatsEntity

@Serializable
internal data class DumbScenarioBackup(val version: Int, val screenWidth: Int, val screenHeight: Int, val dumbScenario: DumbScenarioWithActions)
@Serializable
internal data class DumbScenarioWithActions(val scenario: DumbScenarioEntity, val dumbActions: List<DumbActionWithFingers>, val stats: DumbScenarioStatsEntity?)
@Serializable
internal data class DumbActionWithFingers(val action: DumbActionEntity, val gestureFingers: List<DumbGestureFingerWithMotions>)
@Serializable
internal data class DumbActionEntity(
    val id: Long, val dumbScenarioId: Long, val priority: Int = 0, val name: String, val type: DumbActionType,
    val repeatCount: Int? = null, val isRepeatInfinite: Boolean? = null, val repeatDelay: Long? = null,
    val pressDuration: Long? = null, val x: Int? = null, val y: Int? = null, val pauseDuration: Long? = null,
)
internal enum class DumbActionType { CLICK, GESTURE, PAUSE }
@Serializable
internal data class DumbGestureFingerWithMotions(val finger: DumbGestureFingerEntity, val motions: List<DumbGestureFingerMotionEntity>)
@Serializable
internal data class DumbGestureFingerEntity(val id: Long, val dumbActionId: Long, val index: Int, val startTimeMs: Long)
@Serializable
internal data class DumbGestureFingerMotionEntity(
    val id: Long, val fingerId: Long, val index: Int, val motionType: DumbGestureFingerMotionType,
    val durationMs: Long? = null, val x: Int? = null, val y: Int? = null,
)
internal enum class DumbGestureFingerMotionType { MOVE, PRESS, RELEASE, WAIT }
