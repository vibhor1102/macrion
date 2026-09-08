# Device Workarounds & Input Safety

Automating user input on Android involves deep interaction with the operating system's input pipeline. This page documents Macrion's input safety mechanisms, hardware kill-switches, and OEM-specific workarounds designed to keep your device responsive and protected against software deadlocks.

---

## The Android 15 Pixel Touchscreen Freeze Bug

On Google Pixel smartphones running Android 15 (`API 35`), an OS-level deadlock can occur when accessibility services inject synthetic touch gestures.

### The Problem ([Google Issue #384188031](https://issuetracker.google.com/issues/384188031))
When an app calls Android's `AccessibilityService.dispatchGesture()` repeatedly, Android 15's native `InputDispatcher` thread can enter a state where physical touchscreen inputs are completely ignored. The phone appears completely frozen to human touch, even though the operating system and background apps continue running normally.

### Hardware-Specific Detection
Because this issue is specific to Google's input driver stack in Android 15, Macrion dynamically checks your device parameters:

```kotlin
fun isImpactedByInputBlock(): Boolean =
    Build.VERSION.SDK_INT == Build.VERSION_CODES.VANILLA_ICE_CREAM
        && Build.BRAND == "google"
        && Build.MODEL.lowercase().contains("pixel")
```

The setting **Unblock touch screen** (`inputBlockWorkaround`) will **only appear in Settings on affected devices**. On all other smartphones and earlier Android versions, this option remains completely hidden.

### How the Unblock Workaround Operates
When **Unblock touch screen** is enabled, Macrion activates the `UnblockGestureScheduler`:

1. **Timer Interval**: The scheduler monitors execution and triggers every 10 seconds (`UNBLOCK_GESTURE_DELAY_MS = 10000L`).
2. **Multi-Stroke Injection**: It builds a multi-point micro-gesture at the very top-left pixel corner of the display:
   - Stroke 1: `(1.0, 1.0)`
   - Stroke 2: `(1.0, 3.0)`
   - Stroke 3: `(2.0, 2.0)`
3. **Queue Flushing**: When Android's `InputDispatcher` receives simultaneous multi-finger gesture events, it is forced to flush and reset its internal pointer queue. This immediately clears any pending deadlock and restores human touch responsiveness.

---

## Hardware Emergency Stop (Volume Down)

When an aggressive auto-click scenario is tapping the screen dozens of times per second, attempting to touch an on-screen "Stop" button can be nearly impossible because synthetic gestures continuously consume touch focus.

### The Physical Kill-Switch

Macrion binds an OS-level key interceptor in `KeyBinding.kt`:

```kotlin
fun KeyEvent.isStopScenarioKey(): Boolean =
    keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
```

Whenever a scenario is running—whether it is a Simple coordinate clicker or an advanced Vision automation—**pressing the physical Volume Down key instantly halts execution and dismisses the overlay.**

::: tip No Root or Setup Needed
This hardware key interceptor is handled directly by Macrion's Accessibility Service. It operates globally across all games and apps without needing any special key remapper.
:::

---

## Accidental Stop Protection

For long, unattended automation sessions (such as overnight resource farming or marathon grinding loops), an accidental brush against the screen could inadvertently hit the on-screen Stop button and terminate a multi-hour run.

### Confirm Before Stopping (`isStopConfirmationEnabled`)

When this setting is toggled on:
- Tapping the **`⏹` Stop** button on the floating overlay toolbar will not immediately exit.
- Macrion displays an alert confirmation dialog: *"Are you sure you want to stop Macrion?"*
- You must explicitly tap **Stop** on the dialog to terminate execution.
- If tapped accidentally, tapping outside the dialog or pressing **Cancel** allows the scenario to keep running uninterrupted.

---

## Anti-Detection Randomization

Many game engines monitor touch coordinates and press durations to detect mechanical, automated players. Macrion integrates an engine-level randomizer in `RandomizerConfig.kt`:

| Parameter | Variance Range | How It Protects You |
| :--- | :---: | :--- |
| **Position Offset** | `±5 pixels` | Introduces subtle coordinate jitter so taps never strike the exact same mathematical pixel twice in a row. |
| **Stroke Duration** | `±5 ms` | Varies the physical hold down duration of taps and swipes so touches mimic human finger contact variance. |
| **Pause Delay** | `±5 ms` | Slightly fluctuates delay intervals between actions to eliminate robotic, clock-synced timing patterns. |

Randomization can be toggled on or off on a per-scenario basis in scenario settings.

---

## Related Documentation

- **[Settings Catalog](/documentation/settings/overview)** — Exhaustive table of all settings and persistence keys.
- **[Screen Capture & Display Modes](/documentation/settings/screen-capture)** — Technical overview of frame capture and display compatibility.
- **[Diagnostics & Privacy](/documentation/settings/diagnostics)** — Local on-device crash logs and privacy protections.
