# Macrion Documentation Reference

Welcome to the **Macrion Documentation Reference**.

While the [Guides](/guides/getting-started/quick-start) provide tutorial-style walkthroughs for everyday automation workflows, this reference section serves as an exhaustive technical specification of every configuration setting, vision detector, trigger, execution action, architectural engine subsystem, and integration hook in Macrion.

---

## Documentation Sections

### ⚙️ [App Settings & Safety](/documentation/settings/overview)
Comprehensive technical catalog of Macrion's global configuration options, hardware safety systems, and display capture pipeline:
- **[Settings Catalog](/documentation/settings/overview)**: Complete reference of all `SettingsDataSource` keys, defaults, and scenario list filter/sort options.
- **[Screen Capture & Display Modes](/documentation/settings/screen-capture)**: `MediaProjection` frame streaming, Android 14/15 Partial Screen Sharing pitfalls, and display config enforcement.
- **[Device Workarounds & Input Safety](/documentation/settings/input-safety)**: Google Pixel Android 15 `InputDispatcher` freeze bug ([#384188031](https://issuetracker.google.com/issues/384188031)), `UnblockGestureScheduler` multi-tap workaround, and Volume Down hardware kill-switch.
- **[Diagnostics & Privacy](/documentation/settings/diagnostics)**: Local-first 7-day crash reports, zero telemetry guarantee, and automated GitHub bug report generator.

---

### 👁️ [Vision & Screen Conditions](/documentation/conditions/)
Detailed specifications of all real-time visual detection primitives evaluated by Macrion's OpenCV and neural inference engines:
- **[Vision Engine Overview](/documentation/conditions/)**: Real-time frame ingestion, AND/OR short-circuiting, positive vs. negative detection (`shouldBeDetected`), and confidence thresholds.
- **[Image Conditions](/documentation/conditions/condition-image)**: Exact Area, In-Area, and Full Screen template matching using normalized cross-correlation (`TM_CCOEFF_NORMED`).
- **[Color Conditions](/documentation/conditions/condition-color)**: Single-pixel vs. area average sampling and Euclidean RGB distance matching.
- **[Text Conditions (OCR)](/documentation/conditions/condition-text)**: Tencent NCNN neural OCR inference (`det.ncnn` / `rec.ncnn`), 11 language alphabets, and fuzzy Levenshtein distance matching.
- **[Number Conditions](/documentation/conditions/condition-number)**: Streamlined numeric OCR, comparison operators (`=`, `≠`, `>`, `<`, `≥`, `≤`), and dynamic counter variables.
- **[Resolution & Scaling](/documentation/conditions/resolution-scaling)**: The two-way scaling pipeline (`ScalingManager`) balancing vision performance with exact physical touch injection.

---

### ⏱️ [Event Triggers (Screen-Independent)](/documentation/triggers/)
Evaluating events without capturing screen frames, achieving near-zero battery and CPU consumption:
- **[Triggers Overview](/documentation/triggers/)**: Architecture of `TriggerEvent` vs. vision conditions, zero-screen-capture efficiency, and hybrid scenarios.
- **[Timer Triggers](/documentation/triggers/trigger-timer)**: `OnTimerReached`, one-shot countdowns vs. auto-restarting periodic loops, and clock drift protection.
- **[Counter Triggers](/documentation/triggers/trigger-counter)**: `OnCounterCountReached`, double-precision mathematical comparisons, and loop iteration limits.
- **[Broadcast Intent Triggers](/documentation/triggers/trigger-broadcast)**: `OnBroadcastReceived`, dynamic `SafeBroadcastReceiver` registration, Tasker intent dispatch, and ADB CLI triggering.

---

### 🧠 [Event Architecture & Execution Logic](/documentation/events/)
The structural hierarchy, priority resolution, and runtime flow control of Macrion scenarios:
- **[Event Architecture](/documentation/events/)**: The three-tier hierarchy (Scenario $\to$ Events $\to$ Conditions & Actions), `ScreenEvent` vs. `TriggerEvent`, and lifecycle validation rules.
- **[Priority & Frame Flow](/documentation/events/priority-and-frame-flow)**: Descending priority resolution, single-frame breaks, and multi-event evaluation via `keepDetecting`.
- **[Event Cooldowns](/documentation/events/cooldowns)**: Refractory timers (`cooldownMs`) in `CooldownsState`, preventing duplicate touch injection during slow UI transitions.
- **[State Machines & Flow Control](/documentation/events/dynamic-flow-control)**: Phase-based event toggling (`ENABLE`, `DISABLE`, `TOGGLE`), building finite state machines, and clean auto-shutdown.

---

### 👆 [Actions & Gestures](/documentation/actions/)
The exhaustive catalog of execution primitives dispatched when conditions pass or triggers fire:
- **[Actions Overview](/documentation/actions/)**: Sequential execution pipeline, coroutine dispatching, anti-detection coordinate/duration randomization, and gesture timeouts.
- **[Touch: Clicks & Swipes](/documentation/actions/action-touch)**: Static (`USER_SELECTED`) vs. dynamic condition tracking (`ON_DETECTED_CONDITION`), pixel offsets (`clickOffset`), press durations, and swipe paths.
- **[Text Injection & Counters](/documentation/actions/action-text)**: Deep accessibility node discovery, direct `ACTION_SET_TEXT`, clipboard paste fallback, IME Enter validation, and `{counter}` variable interpolation.
- **[System Actions](/documentation/actions/action-system)**: Android Back, Home, and Recent Apps global navigation commands.
- **[State & Flow Actions](/documentation/actions/action-state)**: Runtime variable arithmetic (`ChangeCounter`), event state manipulation (`ToggleEvent`), and non-blocking coroutine pauses (`Pause`).
- **[External Integration & Intents](/documentation/actions/action-external)**: Dispatching Android Broadcast/Activity `Intent`s with 8 typed extra formats, status bar `Notification`s, and Tasker/Locale plugin `ExternalAction` queries.

---

### 🛡️ [Integration & Advanced Control](/documentation/advanced/permissions)
Platform security, system automation, hardware optimization, and diagnostic tooling:
- **[Permissions & Security](/documentation/advanced/permissions)**: Android permission architecture (Accessibility, MediaProjection, Overlay, Notifications, Battery Exemptions) and the zero-telemetry guarantee.
- **[Tasker, QS Tile & ADB](/documentation/advanced/external-triggers)**: Two-way Tasker/Locale plugin integration, cryptographic Keystore signing, Quick Settings tile toggling, and ADB command-line automation.
- **[Performance Tuning](/documentation/advanced/performance-tuning)**: Resolution downscaling benchmarks, detection frequency guidelines, CPU multi-threading, and device thermal management.
- **[Live Debugger & Profiling](/documentation/advanced/debug-panel)**: Live on-screen bounding boxes, confidence score overlays, condition latency profiling, and diagnostic dump generation.

---

### 📦 [Data Management & Migration](/documentation/migration/backups)
Managing, exporting, sharing, and safely upgrading scenario libraries:
- **[Backups & Storage](/documentation/migration/backups)**: Android Storage Access Framework (SAF) integration, the `.macrion.zip` container specification, the sub-extension invariant (`.macrion.*`), and atomic import transactions.
- **[Klick'r Migration & Compatibility](/documentation/migration/klickr-migration)**: Automatic migration of legacy Klick'r backups, and the in-memory **Compatibility Projection Engine** for exporting clean, backward-compatible archives without mutating live data.
