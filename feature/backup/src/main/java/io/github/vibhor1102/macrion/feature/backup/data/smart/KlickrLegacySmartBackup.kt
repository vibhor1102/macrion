/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.backup.data.smart

import kotlinx.serialization.json.*

/** Exact schema-23 wire boundary. Native Room defaults must not omit required legacy relations. */
internal fun ScenarioBackup.toKlickrLegacySmartJson(): JsonObject {
    require(version == 23 && format == null)
    val source = Json.encodeToJsonElement(scenario).jsonObject
    return buildJsonObject {
        put("version", version); put("screenWidth", screenWidth); put("screenHeight", screenHeight)
        put("scenario", buildJsonObject {
            put("scenario", source.getValue("scenario").jsonObject.only(legacyScenarioFields))
            put("counters", source.getValue("counters"))
            put("events", JsonArray(source.getValue("events").jsonArray.map { value ->
                val event = value.jsonObject
                buildJsonObject {
                    put("event", event.getValue("event").jsonObject.only(legacyEventFields))
                    put("conditions", JsonArray(event.getValue("conditions").jsonArray.map { value ->
                        val condition = value.jsonObject
                        // A stale operand enum belonging to another condition type is not behavior.
                        val ignored = buildSet {
                            if (condition["type"]?.jsonPrimitive?.content != "ON_COUNTER_REACHED") add("counterOperationValueType")
                            if (condition["type"]?.jsonPrimitive?.content != "ON_NUMBER_DETECTED") add("numberCounterOperationValueType")
                        }
                        JsonObject(condition - ignored).only(legacyConditionFields)
                    }))
                    put("actions", JsonArray(event.getValue("actions").jsonArray.map { value ->
                        val action = value.jsonObject
                        buildJsonObject {
                            put("action", action.getValue("action").jsonObject.only(legacyActionFields))
                            put("intentExtras", action["intentExtras"] ?: JsonArray(emptyList()))
                            put("eventsToggle", action["eventsToggle"] ?: JsonArray(emptyList()))
                        }
                    }))
                }
            }))
        })
    }
}

private fun JsonObject.only(fields: Set<String>): JsonObject = JsonObject(filterKeys { it in fields })

// Frozen field names from Klick'r 4.0.0; the pinned strict consumer tests guard this contract.
internal val legacyActionFields = setOf(
    "id", "eventId", "priority", "name", "type", "clickPositionType", "x", "y", "clickOnConditionId", "pressDuration", "clickOffsetX", "clickOffsetY", "fromX", "fromY", "toX", "toY", "swipeDuration", "pauseDuration", "isAdvanced", "isBroadcast", "intentAction", "componentName", "flags", "toggleAll", "toggleAllType", "counterName", "counterOperation", "counterOperationValueType", "counterOperationValue", "counterOperationCounterName", "notificationMessageText", "notificationImportance", "systemActionType", "textValue", "textValidateInput",
)

internal val legacyConditionFields = setOf(
    "id", "eventId", "name", "type", "priority", "shouldBeDetected", "path", "areaLeft", "areaTop", "areaRight", "areaBottom", "threshold", "detectionType", "detectionAreaLeft", "detectionAreaTop", "detectionAreaRight", "detectionAreaBottom", "broadcastAction", "counterName", "counterComparisonOperation", "counterOperationValueType", "counterValue", "counterOperationCounterName", "timerValueMs", "restartWhenReached", "colorRgba", "numberCounterComparisonOperation", "numberCounterOperationValueType", "numberCounterValue", "numberCounterOperationCounterName", "numberFormatType", "textToDetect", "textAlphabet",
)

internal val legacyEventFields = setOf(
    "id", "scenarioId", "name", "conditionOperator", "priority", "enabledOnStart", "type", "keepDetecting", "detectionCooldownMs",
)

internal val legacyScenarioFields = setOf(
    "id", "name", "detectionQuality", "computeRate", "randomize", "keepScreenOn",
)
