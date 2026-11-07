package io.github.vibhor1102.macrion.feature.backup.data.klickr400

import kotlinx.serialization.Serializable

@Serializable
internal data class ScenarioBackup(val version: Int, val screenWidth: Int, val screenHeight: Int, val scenario: CompleteScenario)
