# Klick’r 4.1.2 consumer snapshot

`DumbScenarioSerializer.kt` is pinned to Nain57/Smart-AutoClicker commit
`699bff818519bc418addb7f0bdad5fbfb6c3f720`, path
`feature/backup/src/main/java/com/buzbuz/smartautoclicker/feature/backup/data/dumb/DumbScenarioSerializer.kt`.
GPL header retained. Adaptations: test namespace, shared Macrion helper imports,
removal of the backup interface, pinned database version 3, and plain serializable
wire types replacing the upstream Room-annotated entities. The consumer parsing
algorithm and its field names are unchanged. No application engine code is used.

The tests run this consumer on Macrion exports and check action count, timing,
coordinates, and repetition. They establish parser compatibility, not APK replay.

## Legacy smart click target reader

`LegacySmartClickReader.kt` copies `deserializeActionClick` from
`core/smart/database/.../serialization/compat/CompatV24Deserializer.kt` at the same
pinned commit. Changes to that method: remove `override`, relocate its imports and
class, and return the shared legacy ActionEntity representation. The original
AND/OR decisions and field reads remain unchanged. The downstream finger builder
is replaced with an argument-capturing stub; this checks target/offset parsing,
not full smart scenario restore or gesture execution. It is deliberately
independent of Macrion's projection and reference-validation helpers.
