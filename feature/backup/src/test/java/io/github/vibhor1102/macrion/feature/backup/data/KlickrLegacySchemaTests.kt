package io.github.vibhor1102.macrion.feature.backup.data

import io.github.vibhor1102.macrion.core.database.DATABASE_VERSION
import io.github.vibhor1102.macrion.core.database.entity.*
import io.github.vibhor1102.macrion.feature.backup.data.smart.ScenarioBackup
import io.github.vibhor1102.macrion.feature.backup.data.smart.ScenarioSerializer
import io.github.vibhor1102.macrion.feature.backup.data.smart.legacyActionFields
import io.github.vibhor1102.macrion.feature.backup.data.smart.legacyConditionFields
import io.github.vibhor1102.macrion.feature.backup.data.smart.legacyEventFields
import io.github.vibhor1102.macrion.feature.backup.data.smart.legacyScenarioFields
import io.github.vibhor1102.macrion.feature.backup.data.klickr400.ActionEntity as LegacyAction
import io.github.vibhor1102.macrion.feature.backup.data.klickr400.ConditionEntity as LegacyCondition
import io.github.vibhor1102.macrion.feature.backup.data.klickr400.EventEntity as LegacyEvent
import io.github.vibhor1102.macrion.feature.backup.data.klickr400.ScenarioEntity as LegacyScenario
import io.github.vibhor1102.macrion.feature.backup.data.klickr400.CompleteScenario as LegacyCompleteScenario
import io.github.vibhor1102.macrion.feature.backup.data.klickr400.CompleteEventEntity as LegacyCompleteEvent
import io.github.vibhor1102.macrion.feature.backup.data.klickr400.CompleteActionEntity as LegacyCompleteAction
import io.github.vibhor1102.macrion.feature.backup.data.klickr400.ScenarioBackup as LegacyBackup
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalSerializationApi::class)
class KlickrLegacySchemaTests {
    @Test fun compatibleExportMatchesStrictOldReaderAndNativeMetadataStaysNative() {
        val source = CompleteScenario(
            ScenarioEntity(1, "Folders", 1200, folders = listOf(ScenarioFolderEntity("Group", 0))),
            listOf(CompleteEventEntity(
                EventEntity(2, 1, "Event", 2, 0, type = EventType.IMAGE_EVENT,
                    keepDetecting = false, detectionCooldownMs = 0, folderName = "Group"),
                listOf(CompleteActionEntity(ActionEntity(3, 2, name = "Wait", type = ActionType.PAUSE,
                    pauseDuration = 100, counterOperationValueType = CounterOperationValueType.CONDITION,
                    externalActionName = "Unused", soundTitle = "Unused"))),
                listOf(ConditionEntity(4, 2, "Image", ConditionType.ON_IMAGE_DETECTED, 0,
                    shouldBeDetected = true, path = "Condition_4.png", areaLeft = 0, areaTop = 0,
                    areaRight = 20, areaBottom = 20, threshold = 4, detectionType = 1,
                    counterOperationValueType = CounterOperationValueType.CONDITION,
                    numberCounterOperationValueType = CounterOperationValueType.CONDITION)),
            )), emptyList(),
        )
        val projection = KlickrCompatibilityProjector.projectSmartScenario(source)
        assertEquals(listOf("Group"), projection.losses.single().componentNames)
        val wire = serialize(ScenarioBackup(null, 23, 1080, 2400, projection.value!!))
        val old = Json.decodeFromString<LegacyBackup>(wire)
        assertEquals(1, old.scenario.events.single().actions.size)
        assertTrue(old.scenario.events.single().actions.single().intentExtras.isEmpty())
        assertTrue(old.scenario.events.single().actions.single().eventsToggle.isEmpty())
        assertNull(old.scenario.events.single().actions.single().action.counterOperationValueType)
        assertNull(old.scenario.events.single().conditions.single().counterOperationValueType)
        assertNull(old.scenario.events.single().conditions.single().numberCounterOperationValueType)

        val native = Json.decodeFromString<ScenarioBackup>(serialize(ScenarioBackup("macrion", DATABASE_VERSION, 1080, 2400, source)))
        assertEquals(source, native.scenario)
    }

    @Test fun newNativeFieldsRequireAnExplicitCompatibilityDecision() {
        assertEquals(LegacyAction.serializer().descriptor.names(), legacyActionFields)
        assertEquals(LegacyCondition.serializer().descriptor.names(), legacyConditionFields)
        assertEquals(LegacyEvent.serializer().descriptor.names(), legacyEventFields)
        assertEquals(LegacyScenario.serializer().descriptor.names(), legacyScenarioFields)
        contract(ActionEntity.serializer().descriptor, LegacyAction.serializer().descriptor, setOf(
            "swipePath", "externalActionName", "soundUri", "soundTitle", "screenshotFolderUri", "screenshotFolderName",
            "waitBeforeMs", "waitAfterMs", "positionCounters", "fromCounters", "toCounters",
            "counterOperationConditionId", "counterOperationConditionPositionType",
        ))
        contract(ConditionEntity.serializer().descriptor, LegacyCondition.serializer().descriptor, setOf("computeRate"))
        contract(EventEntity.serializer().descriptor, LegacyEvent.serializer().descriptor, setOf("folderName"))
        contract(ScenarioEntity.serializer().descriptor, LegacyScenario.serializer().descriptor, setOf("folders"))
        contract(CompleteActionEntity.serializer().descriptor, LegacyCompleteAction.serializer().descriptor, setOf("splitItems"))
        contract(CompleteEventEntity.serializer().descriptor, LegacyCompleteEvent.serializer().descriptor)
        contract(CompleteScenario.serializer().descriptor, LegacyCompleteScenario.serializer().descriptor)
        contract(CountersEntity.serializer().descriptor, io.github.vibhor1102.macrion.feature.backup.data.klickr400.CountersEntity.serializer().descriptor)
        contract(IntentExtraEntity.serializer().descriptor, io.github.vibhor1102.macrion.feature.backup.data.klickr400.IntentExtraEntity.serializer().descriptor)
        contract(EventToggleEntity.serializer().descriptor, io.github.vibhor1102.macrion.feature.backup.data.klickr400.EventToggleEntity.serializer().descriptor)
    }

    @Test fun newEnumValuesRequireAnExplicitCompatibilityDecision() {
        assertEquals(io.github.vibhor1102.macrion.feature.backup.data.klickr400.ActionType.entries.map { it.name }.toSet() +
            setOf("EXTERNAL_ACTION", "PLAY_SOUND", "CAPTURE_SCREENSHOT", "SPLIT_ACTION"), ActionType.entries.map { it.name }.toSet())
        assertEquals(io.github.vibhor1102.macrion.feature.backup.data.klickr400.CounterOperationValueType.entries.map { it.name }.toSet() +
            "CONDITION", CounterOperationValueType.entries.map { it.name }.toSet())
        assertEquals(io.github.vibhor1102.macrion.feature.backup.data.klickr400.ConditionType.entries.map { it.name }, ConditionType.entries.map { it.name })
        assertEquals(io.github.vibhor1102.macrion.feature.backup.data.klickr400.SystemActionType.entries.map { it.name }, SystemActionType.entries.map { it.name })
    }

    private fun contract(native: SerialDescriptor, legacy: SerialDescriptor, nativeOnly: Set<String> = emptySet()) {
        assertEquals("Review the legacy export policy when changing ${native.serialName}", legacy.names() + nativeOnly, native.names())
    }

    private fun SerialDescriptor.names() = (0 until elementsCount).map(::getElementName).toSet()

    private fun serialize(backup: ScenarioBackup): String = ByteArrayOutputStream().let {
        ScenarioSerializer().serialize(backup, it)
        it.toString(Charsets.UTF_8.name())
    }
}
