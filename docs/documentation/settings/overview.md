# Settings Catalog

This page provides an exhaustive technical specification of all configuration options available in Macrion's **Settings** screen (`SettingsActivity`), their storage keys, default values, and behavioral effects across the app.

---

## Settings Reference Table

All user preferences are persisted asynchronously via Android Jetpack **Preferences DataStore** (`settings.preferences_pb`) on an IO dispatcher.

| Setting | DataStore Key | Default | Hardware / OS Condition | Purpose & Behavioral Impact |
| :--- | :--- | :---: | :--- | :--- |
| **Show Scenarios filters** | `isFilterScenarioUiEnabled` | `true` | All devices | Displays or hides the scenario type filter chips (All, Smart, Simple) and sorting controls at the top of the main scenario list. |
| **Show scenario switcher** | `isScenarioSwitcherEnabled` | `false` | All devices | Adds the Scenario Switcher button (`⇄`) to the floating overlay toolbar, allowing instant switching between saved scenarios without returning to the main app. |
| **Show Macrion home button** | `isHomeButtonEnabled` | `false` | All devices | Adds the Home button (`🏠`) to the floating overlay toolbar. Tapping it immediately halts execution and brings Macrion's dashboard to the foreground. |
| **Confirm before stopping** | `isStopConfirmationEnabled` | `false` | All devices | Displays a confirmation modal dialog before stopping a running scenario from the overlay toolbar. Prevents accidental interruptions during long unattended farming sessions. |
| **Legacy Action UI** | `isLegacyActionUiEnabled` | `false` | All devices | Replaces the bottom floating horizontal action carousel with a full-screen vertical list dialog (`SmartActionsLegacyDialog`). Ideal for dense scenarios containing dozens of sequential actions. |
| **Legacy Notification UI** | `isLegacyNotificationUiEnabled` | `false` | All devices | Renders standard system action buttons instead of compact icon action buttons in the ongoing foreground service notification. |
| **Force entire screen** | `forceEntireScreen` | `false` | Android 15+ (API 35) | Bypasses Android 15's "Partial Screen Sharing" app picker, forcing the screen capture prompt to record the entire display. |
| **Unblock touch screen** | `inputBlockWorkaround` | `false` | Google Pixel on Android 15 | Injects periodic 3-stroke micro-gestures every 10 seconds to circumvent the Android 15 `InputDispatcher` touchscreen deadlock bug ([Google Issue #384188031](https://issuetracker.google.com/issues/384188031)). |

---

## Scenario Sorting & Filtering Controls

When **Show Scenarios filters** is enabled, the main dashboard exposes quick-filtering chips and sorting headers backed by `ScenarioSortSettings`:

### Filter Modes
- **Show Smart Scenarios**: Toggles visibility of vision-based scenarios (OpenCV template matching, color sampling, neural OCR).
- **Show Simple Scenarios**: Toggles visibility of coordinate-based clicker scenarios (`DumbScenario`).

### Sort Criteria
- **Name**: Alphabetical sorting by scenario name (A–Z or Z–A).
- **Last Used**: Orders scenarios by their most recent execution timestamp.
- **Date Created**: Orders scenarios by original creation date.
- **Inverted Order**: Reverses any of the above sort criteria.

---

## Internal Storage & Migration Architecture

Macrion stores all configuration keys within the `settings` Preferences DataStore namespace. 

### Backward Compatibility & Migration
When upgrading from older versions or migrating historical Klick'r configurations:
1. `LegacySettingsMigration` executes on the IO dispatcher before preferences are first read.
2. Legacy `SharedPreferences` keys are read, mapped to their modern DataStore counterparts, and automatically committed.
3. The legacy preference file is safely cleaned up once migration succeeds.

---

## Related Documentation

- **[Screen Capture & Display Modes](/documentation/settings/screen-capture)** — In-depth technical breakdown of `MediaProjection`, Android 14/15 restrictions, and default display configuration.
- **[Device Workarounds & Input Safety](/documentation/settings/input-safety)** — Detailed look at the Google Pixel Android 15 input freeze bug, volume down kill-switch, and stop protection.
- **[Diagnostics & Privacy](/documentation/settings/diagnostics)** — Local on-device crash logging, data retention policies, and GitHub bug report generation.
