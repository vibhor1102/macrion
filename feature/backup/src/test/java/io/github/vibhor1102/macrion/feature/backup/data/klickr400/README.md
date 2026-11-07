# Klick’r 4.0 smart schema contract

Entity fields, required/default values and enums are copied from the 4.0.0 tag of
Nain57/Smart-AutoClicker, under core/smart/database/.../entity/. GPL headers retained.
Only the package/imports, Room annotations and EntityWithId interface were removed.
The JSON field declarations and defaults are unchanged. The small root backup
wrapper matches the four serialized root fields. Tests use Kotlin serialization's
strict default Json decoder, as Klick’r 4.0's KotlinDeserializer does for schema 23.
This checks the full smart wire schema, including required empty relations; it is
not a detector/engine snapshot or an APK replay test.
