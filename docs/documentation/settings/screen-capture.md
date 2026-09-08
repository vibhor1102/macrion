# Screen Capture & Display Modes

Macrion's vision engine depends on Android's **`MediaProjection`** API to inspect display contents in real time. This page explains how screen capture works under the hood, how Android 14 and 15 changed screen recording behavior, and how the **Force entire screen** setting guarantees reliable automation.

---

## The Screen Capture Architecture

Macrion divides its scenarios into two distinct execution tiers:

1. **Simple (Coordinate-Based) Scenarios**: Only require the Android Accessibility Service. They inject touches directly into fixed `(X, Y)` coordinates without capturing or analyzing display pixels.
2. **Smart (Vision-Based) Scenarios**: Require both the Accessibility Service (to inject touch gestures) and a **`MediaProjection` token** (to capture and analyze screen frames using OpenCV and NCNN neural OCR).

### Real-Time Frame Ingestion Pipeline

When a Smart Scenario starts:
1. Macrion requests a screen capture token via `MediaProjectionManager.createScreenCaptureIntent()`.
2. A virtual display (`VirtualDisplay`) is instantiated, continuously feeding screen buffer frames into an `ImageReader`.
3. Captured frames are routed to `ConditionsVerifier`, which passes bitmaps to the template matcher (OpenCV) or OCR engine (NCNN) to evaluate condition criteria.
4. All frame capture and image processing occur **100% on-device**. No images or pixels are ever saved to disk or transmitted across the network.

---

## The Android 14 & 15 Paradigm: Partial Screen Sharing

Starting with Android 14 and finalized in Android 15 (`API 35`), Google introduced **Partial Screen Sharing**. 

Instead of automatically capturing the whole screen, the system prompt presents a selector dropdown:

```text
Share your screen
[ A single app ▼ ]   or   [ Entire screen ]
```

### Why "A Single App" Breaks Automation

If a user leaves the default setting on "A single app":

* **Activity Transitions & Popups**: If a game launches an authentication webview, an in-app billing window, or a secondary Activity that runs in a different task, the virtual display stream halts or feeds blank black frames.
* **Multi-App Routines**: Scenarios that transition between multiple apps (such as claiming daily rewards across two games or interacting with system settings) cannot detect the second app.
* **System Dialogs**: System permission popups, volume bars, and overlay notifications cannot be detected.

---

## The "Force Entire Screen" Solution

To solve this fragmentation on modern Android versions, Macrion includes the **Force entire screen** setting.

### How It Works Under the Hood

When **Force entire screen** is enabled on Android 15+ (`Build.VERSION_CODES.VANILLA_ICE_CREAM`), Macrion modifies the screen capture intent creation in `MediaProjectionRequest.kt`:

```kotlin
private fun MediaProjectionManager.createScreenCaptureIntentCompat(forceEntireScreen: Boolean): Intent =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM && forceEntireScreen)
        createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay())
    else 
        createScreenCaptureIntent()
```

By providing `MediaProjectionConfig.createConfigForDefaultDisplay()`, Android is instructed to bypass the "Single app" dropdown entirely. The system dialog prompts directly for whole-display capture, ensuring uninterrupted vision across all apps, sub-windows, and orientations.

::: tip When to Enable
If your device runs Android 15 or newer and you notice image detection freezing when switching menus or apps, turn on **Force entire screen** in Macrion Settings.
:::

---

## Resolution, Orientation & Density Scaling

- **Screen Rotation**: When your phone rotates between portrait and landscape, the system reconfigures the virtual display buffer dimensions. Macrion automatically catches orientation updates and updates detection coordinate spaces accordingly.
- **Resolution Scaling (`ScalingManager`)**: Scenarios configured on one display resolution (e.g. 1080p) can adapt when shared or run on different display densities (e.g. 1440p) by scaling template bitmaps proportionally.

---

## Related Documentation

- **[Settings Catalog](/documentation/settings/overview)** — Complete reference table of all settings and persistence keys.
- **[Device Workarounds & Input Safety](/documentation/settings/input-safety)** — Details on the Android 15 Pixel touch freeze issue and emergency stop mechanisms.
- **[Conditions Overview](/documentation/conditions/)** — How captured bitmaps are evaluated by OpenCV and neural OCR engines.
