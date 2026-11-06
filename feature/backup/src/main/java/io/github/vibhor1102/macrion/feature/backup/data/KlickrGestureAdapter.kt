/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.backup.data

import io.github.vibhor1102.macrion.core.base.gesture.MAX_TOUCH_DURATION_MS
import kotlinx.serialization.json.*

/** Converts only gestures with exactly representable contact, position, and timing semantics. */
internal object KlickrGestureAdapter {
    private val fingerKeys = setOf("id", "actionId", "dumbActionId", "index", "startTimeMs")
    private val motionKeys = setOf("id", "fingerId", "index", "motionType", "durationMs", "x", "y",
        "xCoordinateType", "yCoordinateType", "conditionId", "xLiteral", "xCounter", "yLiteral", "yCounter",
        "xOffsetCoordinateType", "yOffsetCoordinateType", "xOffsetLiteral", "xOffsetCounter",
        "yOffsetLiteral", "yOffsetCounter", "releaseHoldType", "holdDurationMs")

    fun convert(complete: JsonObject, dumb: Boolean): JsonObject {
        val action = complete.obj("action")
        if (action.text("type") !in setOf("CLICK", "GESTURE")) {
            val relation = complete["gestureFingers"]
            require(relation == null || (relation is JsonArray && relation.isEmpty())) { "gestureFingers on a non-touch action" }
            return JsonObject(complete - "gestureFingers")
        }
        val fingers = requireNotNull(complete["gestureFingers"] as? JsonArray) { "gestureFingers" }
        require(fingers.size in 1..5) { "finger count" }
        if (action.text("type") == "CLICK") require(fingers.size == 1) { "click finger count" }
        val indexed = fingers.map { it.jsonObject }.sortedBy { it.obj("finger").long("index") }
        require(indexed.map { it.obj("finger").long("index") }.distinct().size == indexed.size) { "finger index" }
        val strokes = indexed.map { finger ->
            require(finger.keys.all { it in setOf("finger", "motions") }) { "unknown finger field" }
            val info = finger.obj("finger")
            require(info.keys.all { it in fingerKeys }) { "unknown finger field" }
            require(info.long(if (dumb) "dumbActionId" else "actionId") == action.long("id")) { "finger owner" }
            val start = info.long("startTimeMs")
            require(start >= 0) { "startTimeMs" }
            val motions = finger.array("motions").map { it.jsonObject }.sortedBy { it.long("index") }
            require(motions.map { it.long("index") }.distinct().size == motions.size) { "motion index" }
            require(motions.all { it.keys.all { key -> key in motionKeys } && it.long("fingerId") == info.long("id") }) { "unknown motion field or owner" }
            require(motions.size in 2..3 && motions.first().text("motionType") == "PRESS" &&
                motions.last().text("motionType") == "RELEASE") { "timed movement sequence" }
            val release = motions.last()
            require(release["releaseHoldType"] == null || release["releaseHoldType"] == JsonNull) { "persistent hold" }
            require(release["holdDurationMs"] == null || release["holdDurationMs"] == JsonNull) { "persistent hold" }
            val middle = motions.getOrNull(1).takeIf { motions.size == 3 }
            require(middle == null || middle.text("motionType") in setOf("MOVE", "WAIT")) { "motion type" }
            val duration = middle?.long("durationMs") ?: 1L
            require(duration in 1..MAX_TOUCH_DURATION_MS && start <= MAX_TOUCH_DURATION_MS - duration) { "gesture duration" }
            val press = position(motions.first(), dumb, allowCondition = middle?.text("motionType") != "MOVE")
            val move = middle?.takeIf { it.text("motionType") == "MOVE" }?.let { position(it, dumb, allowCondition = false) }
            buildJsonObject {
                put("type", if (move == null) "CLICK" else "SWIPE")
                put("duration", duration)
                put("startOffset", start)
                put("priority", info.long("index"))
                put("id", info.long("id"))
                put("actionId", action.long("id"))
                put("name", action.text("name"))
                press.forEach { (key, value) -> put(key, value) }
                if (move != null) {
                    put("toX", move.getValue("fromX"))
                    put("toY", move.getValue("fromY"))
                }
            }
        }
        require(strokes.map { it.long("id") }.distinct().size == strokes.size) { "finger identifiers" }
        val resultAction = action.toMutableMap()
        if (strokes.size == 1) {
            val stroke = strokes.single()
            resultAction["type"] = stroke.getValue("type")
            resultAction["waitBeforeMs"] = stroke.getValue("startOffset")
            if (stroke.text("type") == "CLICK") {
                resultAction["x"] = stroke["fromX"] ?: JsonNull
                resultAction["y"] = stroke["fromY"] ?: JsonNull
                resultAction["pressDuration"] = stroke.getValue("duration")
                if (!dumb) for (key in listOf("clickPositionType", "clickOnConditionId", "clickOffsetX", "clickOffsetY")) {
                    stroke[key]?.let { resultAction[key] = it }
                }
            } else {
                for (key in listOf("fromX", "fromY", "toX", "toY")) resultAction[key] = stroke.getValue(key)
                resultAction["swipeDuration"] = stroke.getValue("duration")
            }
        } else resultAction["type"] = JsonPrimitive("SPLIT_ACTION")
        return buildJsonObject {
            complete.filterKeys { it !in setOf("action", "gestureFingers") }.forEach { (key, value) -> put(key, value) }
            put("action", JsonObject(resultAction))
            if (strokes.size > 1) put("splitItems", JsonArray(strokes.map { stroke ->
                // Simple split items do not have the smart click-position fields.
                if (dumb) JsonObject(stroke.filterKeys { !it.startsWith("click") }) else stroke
            }))
        }
    }

    private fun position(motion: JsonObject, dumb: Boolean, allowCondition: Boolean): JsonObject {
        if (dumb) return buildJsonObject {
            put("fromX", motion.coordinate("x")); put("fromY", motion.coordinate("y"))
        }
        val xType = motion.text("xCoordinateType")
        val yType = motion.text("yCoordinateType")
        val offsetX = offset(motion, "x")
        val offsetY = offset(motion, "y")
        return buildJsonObject {
            if (xType == "CONDITION" && yType == "CONDITION" && allowCondition) {
                put("clickPositionType", "ON_DETECTED_CONDITION")
                motion["conditionId"]?.let { put("clickOnConditionId", it) }
                put("clickOffsetX", offsetX); put("clickOffsetY", offsetY)
            } else {
                require(xType == "LITERAL" && yType == "LITERAL") { "dynamic gesture coordinates" }
                val x = motion.coordinate("xLiteral").toLong() + offsetX
                val y = motion.coordinate("yLiteral").toLong() + offsetY
                require(x in 0..Int.MAX_VALUE.toLong() && y in 0..Int.MAX_VALUE.toLong()) { "coordinates" }
                put("fromX", x.toInt()); put("fromY", y.toInt())
                put("clickPositionType", "USER_SELECTED")
            }
        }
    }

    private fun offset(motion: JsonObject, axis: String): Int {
        val type = motion["${axis}OffsetCoordinateType"]
        require(type == null || type == JsonNull || type.jsonPrimitive.content == "LITERAL") { "dynamic offset" }
        if (type == null || type == JsonNull) return 0
        return motion["${axis}OffsetLiteral"]?.takeUnless { it == JsonNull }?.jsonPrimitive?.int ?: 0
    }
}

internal fun JsonObject.obj(key: String): JsonObject = getValue(key).jsonObject
internal fun JsonObject.array(key: String): JsonArray = getValue(key).jsonArray
internal fun JsonObject.text(key: String): String = getValue(key).jsonPrimitive.content
internal fun JsonObject.long(key: String): Long = getValue(key).jsonPrimitive.long
private fun JsonObject.coordinate(key: String): Int = getValue(key).jsonPrimitive.int.also { require(it >= 0) { key } }
