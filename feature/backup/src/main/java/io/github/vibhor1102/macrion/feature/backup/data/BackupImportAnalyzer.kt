/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.backup.data

import io.github.vibhor1102.macrion.core.database.DATABASE_VERSION
import io.github.vibhor1102.macrion.core.database.entity.*
import io.github.vibhor1102.macrion.core.domain.model.action.mapper.isUsableInBackup
import io.github.vibhor1102.macrion.core.domain.model.condition.isUsableInBackup
import io.github.vibhor1102.macrion.core.dumb.data.database.*
import io.github.vibhor1102.macrion.core.dumb.domain.model.isUsableInBackup
import io.github.vibhor1102.macrion.feature.backup.data.base.*
import io.github.vibhor1102.macrion.feature.backup.data.dumb.DumbScenarioBackup
import io.github.vibhor1102.macrion.feature.backup.data.dumb.DumbScenarioSerializer
import io.github.vibhor1102.macrion.feature.backup.data.smart.ScenarioBackup
import io.github.vibhor1102.macrion.feature.backup.data.smart.ScenarioSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.json.*

/** Inspection never inserts data. Any loss or unknown version requires a separate user decision. */
@OptIn(ExperimentalSerializationApi::class)
internal class BackupImportAnalyzer {
    private val issues = mutableListOf<ImportIssue>()
    private var scenarioName = ""

    fun analyze(source: JsonObject, format: BackupArchiveFormat, dumb: Boolean): NormalizedScenario {
        issues.clear()
        scenarioName = runCatching { source.obj(if (dumb) "dumbScenario" else "scenario").obj("scenario").text("name") }
            .getOrDefault("?")
        val output = try {
            val version = source.long("version").also { require(it > 0 && it <= Int.MAX_VALUE) }.toInt()
            val known = if (format == BackupArchiveFormat.MACRION_NATIVE) {
                if (dumb) version in 1..DUMB_DATABASE_VERSION else version in 8..DATABASE_VERSION
            } else if (dumb) version in setOf(1, 2, 3) else version in 8..25
            if (!known) issue("", ImportIssueKind.VERSION, "${if (format == BackupArchiveFormat.MACRION_NATIVE) "Macrion" else "Klick’r"}: $version")
            val rootKeys = setOf("format", "version", "screenWidth", "screenHeight", if (dumb) "dumbScenario" else "scenario")
            (source.keys - rootKeys).forEach { issue("", ImportIssueKind.FIELD, it) }
            if (dumb) normalizeDumb(source, format, version) else normalizeSmart(source, format, version)
        } catch (error: IllegalArgumentException) {
            issue("", ImportIssueKind.SCENARIO, "${error.message.orEmpty().take(160)}")
            null
        } catch (error: NoSuchElementException) {
            issue("", ImportIssueKind.SCENARIO, "${error.message.orEmpty().take(160)}")
            null
        }
        if (output == null && issues.none { it.kind == ImportIssueKind.SCENARIO }) issue("", ImportIssueKind.SCENARIO, "")
        return NormalizedScenario(output, issues.toList())
    }

    private fun normalizeSmart(source: JsonObject, format: BackupArchiveFormat, version: Int): JsonObject? {
        val body = source.obj("scenario")
        if (!fields(body, setOf("scenario", "events", "counters") + if (version < 13) setOf("endConditions") else emptySet(), "", ImportIssueKind.SCENARIO) ||
            !audit(JsonObject(body.obj("scenario") - if (version < 13) setOf("endConditionOperator") else emptySet()), ScenarioEntity.serializer().descriptor, "", ImportIssueKind.SCENARIO) ||
            !audit(body["counters"] ?: JsonArray(emptyList()), ListSerializer(CountersEntity.serializer()).descriptor, "", ImportIssueKind.SCENARIO)) return null
        if (version < 20) require((body["counters"] as? JsonArray).orEmpty().isEmpty()) { "counter definitions before schema 20" }
        if (version < 13) {
            body.obj("scenario")["endConditionOperator"]?.let { require(it.jsonPrimitive.int in 1..2) { "endConditionOperator" } }
            (body["endConditions"] as? JsonArray).orEmpty().forEach { element ->
                val condition = element.jsonObject
                require(condition.keys.all { it in setOf("id", "scenarioId", "eventId", "executions") }) { "endConditions" }
                condition.long("id"); condition.long("eventId")
                condition["executions"]?.jsonPrimitive?.int
                require(condition.long("scenarioId") == body.obj("scenario").long("id")) { "endConditions.scenarioId" }
            }
        }
        val modern = format == BackupArchiveFormat.KLICKR_COMPATIBLE && version >= 24
        val originalEvents = body.array("events")
        val cleanEvents = originalEvents.mapNotNull { element ->
            val event = element as? JsonObject ?: return@mapNotNull omittedEvent("?", "event layout")
            val name = runCatching { event.obj("event").text("name") }.getOrDefault("?")
            try {
                if (!fields(event, setOf("event", "actions", "conditions"), name, ImportIssueKind.EVENT) ||
                    !audit(event.obj("event"), EventEntity.serializer().descriptor, name, ImportIssueKind.EVENT)) return@mapNotNull null
                // Dropping an unknown condition would broaden the trigger. Drop the entire event instead.
                if (!audit(event.array("conditions"), ListSerializer(ConditionEntity.serializer()).descriptor, name, ImportIssueKind.EVENT)) return@mapNotNull null
                val actions = event.array("actions").mapNotNull { raw ->
                    val actionName = runCatching { raw.jsonObject.obj("action").text("name") }.getOrDefault("?")
                    try {
                        val converted = if (modern || raw.jsonObject.containsKey("gestureFingers")) KlickrGestureAdapter.convert(raw.jsonObject, dumb = false) else raw.jsonObject
                        if (!audit(historicalActionForAudit(converted, version), CompleteActionEntity.serializer().descriptor, actionName, ImportIssueKind.ACTION)) null
                        else {
                            if (modern || (format == BackupArchiveFormat.MACRION_NATIVE && version >= 31)) {
                                require(Json.decodeFromJsonElement<CompleteActionEntity>(converted).isUsableInBackup()) { "incomplete action" }
                            }
                            converted
                        }
                    } catch (error: IllegalArgumentException) {
                        issue(actionName, ImportIssueKind.ACTION, error.message.orEmpty().take(160)); null
                    } catch (error: NoSuchElementException) {
                        issue(actionName, ImportIssueKind.ACTION, error.message.orEmpty().take(160)); null
                    }
                }
                if (actions.isEmpty()) return@mapNotNull omittedEvent(name, "")
                JsonObject(event + ("actions" to JsonArray(actions)))
            } catch (error: IllegalArgumentException) { omittedEvent(name, error.message.orEmpty().take(160)) }
            catch (error: NoSuchElementException) { omittedEvent(name, error.message.orEmpty().take(160)) }
        }
        val cleanBody = JsonObject(body + ("events" to JsonArray(cleanEvents)))
        val canonical = if ((format == BackupArchiveFormat.MACRION_NATIVE && version < 31) ||
            (format == BackupArchiveFormat.KLICKR_COMPATIBLE && version < 24)) {
            ScenarioSerializer().deserialize(JsonObject(source.filterKeys { it in setOf("format", "version", "screenWidth", "screenHeight", "scenario") } +
                ("scenario" to cleanBody)).toString().byteInputStream(), format)?.scenario ?: return null
        } else Json.decodeFromJsonElement<CompleteScenario>(cleanBody)
        if (version < 20) {
            // Historical adapters deliberately create counters, aggregate toggles, and add a stop event.
            // Inspect the migrated native shape, rather than mistaking those migrations for data loss.
            val originalIds = cleanEvents.map { it.obj("event").long("id") }.toSet()
            val kept = canonical.events.filter { event ->
                val original = cleanEvents.firstOrNull { it.obj("event").long("id") == event.event.id }
                if (original == null) true else {
                    val requiredConditions = original.array("conditions").map { it.jsonObject.long("id") }.toSet()
                    val valid = event.conditions.map { it.id }.toSet().containsAll(requiredConditions)
                    if (!valid) omittedEvent(event.event.name, "conditions")
                    val actualIds = event.actions.map { it.action.id }.toSet()
                    original.array("actions").forEach { action ->
                        val value = action.jsonObject.obj("action")
                        if (value.long("id") !in actualIds && !(version < 13 && value.text("type") == "TOGGLE_EVENT")) issue(value.text("name"), ImportIssueKind.ACTION, "legacy conversion")
                    }
                    valid
                }
            }
            cleanEvents.filter { it.obj("event").long("id") !in canonical.events.map { event -> event.event.id }.toSet() }
                .forEach { omittedEvent(it.obj("event").text("name"), "legacy conversion") }
            if (version < 13) {
                val retainedIds = kept.map { it.event.id }.toSet()
                require((body["endConditions"] as? JsonArray).orEmpty().all { it.jsonObject.long("eventId") in retainedIds && it.jsonObject.long("eventId") in originalIds }) { "endConditions.eventId" }
            }
            val native = Json.encodeToJsonElement(ScenarioBackup(MACRION_FORMAT_NAME, DATABASE_VERSION,
                source["screenWidth"]?.jsonPrimitive?.int ?: 0, source["screenHeight"]?.jsonPrimitive?.int ?: 0,
                canonical.copy(events = kept))).jsonObject
            val checked = BackupImportAnalyzer().analyze(native, BackupArchiveFormat.MACRION_NATIVE, false)
            issues += checked.issues
            return checked.json
        }
        require(canonical.counters.size == ((body["counters"] as? JsonArray)?.size ?: 0)) { "counter definitions" }
        val parsedEventIds = canonical.events.map { it.event.id }.toSet()
        cleanEvents.filter { it.obj("event").long("id") !in parsedEventIds }.forEach { omittedEvent(it.obj("event").text("name"), "legacy conversion") }
        var events = canonical.events.mapNotNull { event ->
            val name = event.event.name
            val raw = cleanEvents.first { it.obj("event").long("id") == event.event.id }
            if (raw.array("conditions").size != event.conditions.size ||
                raw.array("conditions").map { it.jsonObject.long("id") }.toSet() != event.conditions.map { it.id }.toSet()) {
                return@mapNotNull omittedEvent(name, "conditions")
            }
            if (event.event.conditionOperator !in 1..2 || event.conditions.isEmpty() || event.conditions.any {
                !it.isUsableInBackup() || it.eventId != event.event.id ||
                    ((it.type in screenConditionTypes) != (event.event.type == EventType.IMAGE_EVENT))
            }) return@mapNotNull omittedEvent(name, "conditions")
            val parsedIds = event.actions.map { it.action.id }.toSet()
            raw.array("actions").filter { it.jsonObject.obj("action").long("id") !in parsedIds }.forEach {
                issue(it.jsonObject.obj("action").text("name"), ImportIssueKind.ACTION, "legacy conversion")
            }
            val actions = event.actions.filter { action ->
                val rawAction = raw.array("actions").first { it.jsonObject.obj("action").long("id") == action.action.id }.jsonObject
                val relationsPreserved = listOf("intentExtras" to action.intentExtras.size, "eventsToggle" to action.eventsToggle.size,
                    "splitItems" to action.splitItems.size).all { (key, count) -> ((rawAction[key] as? JsonArray)?.size ?: 0) == count }
                val valid = relationsPreserved && action.action.eventId == event.event.id && action.isUsableInBackup() &&
                    action.splitItems.all { it.actionId == action.action.id && it.type in setOf(ActionType.CLICK, ActionType.SWIPE) } &&
                    action.intentExtras.all { it.actionId == action.action.id } && action.eventsToggle.all { it.actionId == action.action.id }
                if (!valid) issue(action.action.name, ImportIssueKind.ACTION, action.action.type.name)
                valid
            }
            if (actions.isEmpty()) omittedEvent(name, "") else event.copy(actions = actions)
        }
        // Removing an event can invalidate another event's toggles. Repeat until the graph is stable.
        while (true) {
            val eventIds = events.map { it.event.id }.toSet()
            val next = events.mapNotNull { event ->
                val conditionIds = event.conditions.filter { it.type in screenConditionTypes }.map { it.id }.toSet()
                fun validClick(type: ClickPositionType?, id: Long?): Boolean = type != ClickPositionType.ON_DETECTED_CONDITION ||
                    (event.event.type == EventType.IMAGE_EVENT && ((id == null && event.event.conditionOperator == 2) || id in conditionIds))
                val actions = event.actions.filter { complete ->
                    val action = complete.action
                    val counterNames = canonical.counters.map { it.name }.toSet()
                    val positionNames = listOfNotNull(action.positionCounters, action.fromCounters, action.toCounters).flatMap { it.names() } +
                        complete.splitItems.flatMap { item -> listOfNotNull(item.positionCounters, item.fromCounters, item.toCounters).flatMap { it.names() } }
                    val valid = positionNames.all { it in counterNames } &&
                        (action.counterOperationValueType != CounterOperationValueType.CONDITION ||
                            (event.event.type == EventType.IMAGE_EVENT && event.conditions.any {
                                it.id == action.counterOperationConditionId && it.shouldBeDetected == true
                            })) && validClick(action.clickPositionType, action.clickOnConditionId) &&
                        complete.splitItems.all { validClick(it.clickPositionType, it.clickOnConditionId) } &&
                        (action.type != ActionType.TOGGLE_EVENT || action.toggleAll == true || complete.eventsToggle.all { it.toggleEventId in eventIds })
                    if (!valid) issue(action.name, ImportIssueKind.REFERENCE, "")
                    valid
                }
                if (actions.isEmpty()) omittedEvent(event.event.name, "") else event.copy(actions = actions)
            }
            if (next == events) break
            events = next
        }
        if (events.isEmpty()) return null
        requireUnique(events.map { it.event.id })
        requireUnique(events.flatMap { it.conditions }.map { it.id })
        requireUnique(events.flatMap { it.actions }.map { it.action.id })
        requireUnique(events.flatMap { it.actions }.flatMap { it.splitItems }.map { it.id })
        val normalized = canonical.copy(events = events.sortedBy { it.event.priority }.mapIndexed { index, event ->
            event.copy(event = event.event.copy(priority = index), actions = event.actions.sortedBy { it.action.priority }.mapIndexed { priority, action -> action.copy(action = action.action.copy(priority = priority)) })
        })
        return Json.encodeToJsonElement(ScenarioBackup(MACRION_FORMAT_NAME, DATABASE_VERSION,
            source["screenWidth"]?.jsonPrimitive?.int ?: 0, source["screenHeight"]?.jsonPrimitive?.int ?: 0, normalized)).jsonObject
    }

    private fun normalizeDumb(source: JsonObject, format: BackupArchiveFormat, version: Int): JsonObject? {
        val body = source.obj("dumbScenario")
        if (!fields(body, setOf("scenario", "dumbActions", "stats", "dumbActionsWithSubActions"), "", ImportIssueKind.SCENARIO) ||
            !audit(body.obj("scenario"), DumbScenarioEntity.serializer().descriptor, "", ImportIssueKind.SCENARIO)) return null
        val rawActions = body.array("dumbActions")
        val extras = body["dumbActionsWithSubActions"] as? JsonArray ?: JsonArray(emptyList())
        val converted = rawActions.mapNotNull { raw ->
            val obj = raw as? JsonObject ?: run {
                issue("?", ImportIssueKind.ACTION, "action layout")
                return@mapNotNull null
            }
            val wrapped = obj["action"] is JsonObject
            val base = if (wrapped) obj.obj("action") else obj
            val name = base["name"]?.jsonPrimitive?.content ?: "?"
            try {
                val complete = when {
                    wrapped && base.text("type") == "GESTURE" -> KlickrGestureAdapter.convert(obj, dumb = true)
                    wrapped -> {
                        require(obj.keys.all { it in setOf("action", "gestureFingers") } && (obj["gestureFingers"] as? JsonArray).orEmpty().isEmpty()) { "action relation" }
                        buildJsonObject { put("action", base) }
                    }
                    else -> extras.firstOrNull { extra ->
                        runCatching { extra.jsonObject.obj("action").long("id") == base.long("id") }.getOrDefault(false)
                    }?.jsonObject?.also { require(it.obj("action") == base) { "conflicting action metadata" } }
                        ?: buildJsonObject { put("action", base) }
                }
                // Old simple exports have either spelling for their owner ID.
                val action = complete.obj("action").toMutableMap()
                action.remove("dumb_scenario_id")?.let {
                    require(action["dumbScenarioId"] == null || action["dumbScenarioId"] == it) { "conflicting scenario reference" }
                    action.putIfAbsent("dumbScenarioId", it)
                }
                val normalized = JsonObject(complete + ("action" to JsonObject(action)))
                if (!audit(normalized, DumbActionWithSubActions.serializer().descriptor, name, ImportIssueKind.ACTION)) null
                else {
                    if (!(version in 1..2 && format == BackupArchiveFormat.KLICKR_COMPATIBLE)) {
                        require(Json.decodeFromJsonElement<DumbActionWithSubActions>(normalized).isUsableInBackup()) { "incomplete action" }
                    }
                    normalized
                }
            } catch (error: IllegalArgumentException) { issue(name, ImportIssueKind.ACTION, error.message.orEmpty().take(160)); null }
            catch (error: NoSuchElementException) { issue(name, ImportIssueKind.ACTION, error.message.orEmpty().take(160)); null }
        }
        val actions: List<DumbActionWithSubActions> = if (version in 1..2 && format == BackupArchiveFormat.KLICKR_COMPATIBLE) {
            val flat = JsonArray(converted.map { it.obj("action") })
            val legacy = DumbScenarioSerializer().deserialize(JsonObject(source.filterKeys { it in setOf("format", "version", "screenWidth", "screenHeight", "dumbScenario") } +
                ("dumbScenario" to JsonObject(body + ("dumbActions" to flat)))).toString().byteInputStream(), format) ?: return null
            legacy.dumbScenario.dumbActions.map { DumbActionWithSubActions(it) }
        } else converted.map { Json.decodeFromJsonElement<DumbActionWithSubActions>(it) }
        val parsedIds = actions.map { it.action.id }.toSet()
        converted.filter { it.obj("action").long("id") !in parsedIds }.forEach { issue(it.obj("action").text("name"), ImportIssueKind.ACTION, "legacy conversion") }
        val info = if (version in 1..2 && format == BackupArchiveFormat.KLICKR_COMPATIBLE) {
            // Legacy readers fill the historical scenario defaults too.
            DumbScenarioSerializer().deserialize(source.toString().byteInputStream(), format)?.dumbScenario?.scenario ?: return null
        } else Json.decodeFromJsonElement<DumbScenarioEntity>(body.obj("scenario"))
        val valid = actions.filter {
            val good = it.action.dumbScenarioId == info.id && it.isUsableInBackup() &&
                it.splitItems.all { child -> child.actionId == it.action.id && child.type in setOf(DumbActionType.CLICK, DumbActionType.SWIPE) }
            if (!good) issue(it.action.name, ImportIssueKind.ACTION, it.action.type.name)
            good
        }.sortedBy { it.action.priority }.mapIndexed { index, item -> item.copy(action = item.action.copy(priority = index)) }
        if (valid.isEmpty()) return null
        requireUnique(valid.map { it.action.id }); requireUnique(valid.flatMap { it.splitItems }.map { it.id })
        val result = DumbScenarioWithActions(info, valid.map { it.action }, stats = null, dumbActionsWithSubActions = valid.filter { it.splitItems.isNotEmpty() })
        return Json.encodeToJsonElement(DumbScenarioBackup(MACRION_FORMAT_NAME, DUMB_DATABASE_VERSION,
            source["screenWidth"]?.jsonPrimitive?.int ?: 0, source["screenHeight"]?.jsonPrimitive?.int ?: 0, result)).jsonObject
    }

    private fun historicalActionForAudit(value: JsonObject, version: Int): JsonObject {
        val action = value.obj("action")
        val keys = mutableSetOf<String>()
        if (version < 20 && action.text("type") == "NOTIFICATION") {
            keys += setOf("notificationMessageType", "notificationMessageCounterName")
            require(action["notificationMessageType"]?.jsonPrimitive?.content in NotificationMessageType.entries.map { it.name }) { "notificationMessageType" }
        }
        if (version < 13 && action.text("type") == "TOGGLE_EVENT") {
            keys += setOf("toggleEventId", "toggleEventType")
            action.long("toggleEventId")
            require(action["toggleEventType"]?.jsonPrimitive?.content in EventToggleType.entries.map { it.name }) { "toggleEventType" }
        }
        if (version < 11 && action.text("type") == "CLICK") {
            keys += "clickOnCondition"
            require(action["clickOnCondition"]?.jsonPrimitive?.booleanOrNull != null) { "clickOnCondition" }
        }
        return JsonObject(value + ("action" to JsonObject(action - keys)))
    }

    private fun issue(item: String, kind: ImportIssueKind, detail: String) { issues += ImportIssue(scenarioName, item, kind, detail) }
    private fun omittedEvent(name: String, detail: String): Nothing? { issue(name, ImportIssueKind.EVENT, detail); return null }
    private fun fields(value: JsonObject, allowed: Set<String>, item: String, kind: ImportIssueKind): Boolean {
        val unknown = value.keys - allowed
        unknown.forEach { issue(item, kind, it) }
        return unknown.isEmpty()
    }
    private fun audit(value: JsonElement, descriptor: SerialDescriptor, item: String, kind: ImportIssueKind): Boolean {
        val unknown = unknownFields(value, descriptor)
        unknown.forEach { issue(item, kind, it) }
        return unknown.isEmpty()
    }
    private fun requireUnique(ids: List<Long>) { require(ids.size == ids.distinct().size) { "duplicate identifiers" } }
}

/** Unknown keys and enum values are reported at their owning behavioral component, never defaulted. */
@OptIn(ExperimentalSerializationApi::class)
private fun unknownFields(value: JsonElement, descriptor: SerialDescriptor, path: String = ""): List<String> {
    if (value == JsonNull) return emptyList()
    return when (descriptor.kind) {
        SerialKind.ENUM -> if ((value as? JsonPrimitive)?.content in (0 until descriptor.elementsCount).map(descriptor::getElementName)) emptyList() else listOf("$path=${value.toString().take(80)}")
        StructureKind.LIST -> if (value is JsonArray) value.flatMapIndexed { index, child -> unknownFields(child, descriptor.getElementDescriptor(0), "$path[$index]") } else listOf(path)
        StructureKind.CLASS, StructureKind.OBJECT -> if (value is JsonObject) value.flatMap { (key, child) ->
            val index = descriptor.getElementIndex(key)
            val name = if (path.isEmpty()) key else "$path.$key"
            if (index == kotlinx.serialization.encoding.CompositeDecoder.UNKNOWN_NAME) listOf(name) else unknownFields(child, descriptor.getElementDescriptor(index), name)
        } else listOf(path)
        is PrimitiveKind -> {
            val primitive = value as? JsonPrimitive
            val valid = primitive != null && when (descriptor.kind) {
                PrimitiveKind.STRING -> primitive.isString
                PrimitiveKind.CHAR -> primitive.isString && primitive.content.length == 1
                PrimitiveKind.BOOLEAN -> !primitive.isString && primitive.booleanOrNull != null
                PrimitiveKind.BYTE -> !primitive.isString && primitive.intOrNull?.let { it in Byte.MIN_VALUE..Byte.MAX_VALUE } == true
                PrimitiveKind.SHORT -> !primitive.isString && primitive.intOrNull?.let { it in Short.MIN_VALUE..Short.MAX_VALUE } == true
                PrimitiveKind.INT -> !primitive.isString && primitive.intOrNull != null
                PrimitiveKind.LONG -> !primitive.isString && primitive.longOrNull != null
                PrimitiveKind.FLOAT -> !primitive.isString && primitive.floatOrNull?.isFinite() == true
                PrimitiveKind.DOUBLE -> !primitive.isString && primitive.doubleOrNull?.isFinite() == true
                else -> false
            }
            if (valid) emptyList() else listOf(path)
        }
        else -> emptyList()
    }
}

private val screenConditionTypes = setOf(ConditionType.ON_IMAGE_DETECTED, ConditionType.ON_COLOR_DETECTED,
    ConditionType.ON_TEXT_DETECTED, ConditionType.ON_NUMBER_DETECTED)
