# Permissions, Security & Platform Isolation

Android is built on a sandboxed security architecture where individual applications are strictly isolated from one another. Because Macrion operates as an automation assistant—injecting touch gestures, reading screen content, and interacting with third-party application interfaces—it requires specific system privileges granted by the user.

This document details the internal technical purpose of each permission requested by Macrion, how platform security boundaries are respected, and how Macrion guarantees user privacy with zero data exfiltration.

---

## Permission Architecture Matrix

The table below outlines all permissions utilized across Macrion's feature set:

| Permission | Android API / Constant | Scope | Required For |
| :--- | :--- | :--- | :--- |
| **Accessibility Service** | `android.permission.BIND_ACCESSIBILITY_SERVICE` | Global Device Control | Touch gesture injection (`dispatchGesture`), system navigation (`BACK`, `HOME`, `RECENTS`), text typing, and focus inspection. |
| **Screen Capture** | `android.media.projection.MediaProjection` | Display Frame Stream | Vision-based image, text (OCR), color, and number detection in Smart Scenarios. *(Not needed for simple position-based clicks).* |
| **Display Over Other Apps** | `android.permission.SYSTEM_ALERT_WINDOW` | System Window Overlay | Rendering the floating companion menu, scenario configuration widgets, and live debugging overlays. |
| **Post Notifications** | `android.permission.POST_NOTIFICATIONS` | Status Bar Notifications | Android 13+ foreground service lifecycle persistence, automation alerts, and Tasker launch fallbacks. |
| **Battery Optimization Exemption** | `android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Power Management | Preventing Android Doze mode and OEM task killers from freezing background automation during long runs. |

---

## Detailed Privilege Breakdown

### 1. Accessibility Service (`PermissionAccessibilityService`)

The Accessibility Service is Macrion's foundational interaction engine. Under the Android security framework, third-party applications are normally prohibited from dispatching input events outside their own window boundaries. Only an enabled Accessibility Service possesses the platform authorization to perform cross-application interactions.

#### What Macrion Uses It For
- **Touch & Gesture Dispatch**: Injects single-stroke clicks and continuous vector swipes via `AccessibilityService.dispatchGesture()`.
- **System Navigation**: Triggers global Android commands (`GLOBAL_ACTION_BACK`, `GLOBAL_ACTION_HOME`, `GLOBAL_ACTION_RECENTS`) via `AccessibilityService.performGlobalAction()`.
- **Text Injection**: Dispatches `ACTION_SET_TEXT` or clipboard paste into active input fields found via `findFocus(AccessibilityNodeInfo.FOCUS_INPUT)`.
- **Hardware Kill-Switch**: Listens for Volume Down key events (`KeyEvent.KEYCODE_VOLUME_DOWN`) to immediately halt scenario loops in emergencies.

#### Privacy & Security Architecture
Android displays a standard warning: *"Macrion will have full control of your device."* However, Macrion's accessibility implementation adheres to strict security constraints:
- **Zero Keystroke Logging**: Macrion does not implement `onAccessibilityEvent` listeners for text changes or password entries. It does not monitor or record what you type.
- **Zero Network Telemetry**: Macrion contains no analytics SDKs, trackers, or network sockets. It is physically incapable of transmitting screen or accessibility data off the device.
- **Deterministic Triggering**: Gestures are executed exclusively in response to scenario conditions you explicitly authored.

---

### 2. Screen Capture (`MediaProjection`)

To analyze what is visible on your screen, Macrion's vision engine requests a `MediaProjection` capture token through Android's `MediaProjectionManager`.

```
Screen Capture Flow
 ├── Prompt: Android System Consent Dialog ("Start Recording or Casting?")
 ├── Display Config: Full display capture enforced (createConfigForDefaultDisplay)
 ├── Ingestion: Surface / ImageReader streaming raw frames to Vision Engine
 └── Buffer Lifecycle: Frames analyzed in RAM and recycled immediately (Zero disk writes)
```

#### Android 14 & 15 Isolation Safeguards
Modern Android releases introduced **Partial Screen Sharing**, allowing users to share an individual app rather than the entire display. However, partial capture can cause severe coordinate desynchronization and crashes when automating across apps. 

As documented in [Screen Capture Configuration](/documentation/settings/screen-capture), Macrion explicitly passes:
```kotlin
MediaProjectionConfig.createConfigForDefaultDisplay()
```
This forces the system projection to capture the entire physical display, guaranteeing exact coordinate alignment between image matching and touch injection.

> [!NOTE]
> Screen capture frames are processed strictly in volatile device memory (RAM) and immediately recycled. Macrion **never** saves screenshots, video recordings, or screen buffers to storage.

---

### 3. Display Over Other Apps (`PermissionOverlay`)

The **Display Over Other Apps** permission allows Macrion to spawn windows of type `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`.

#### Purpose & Window Flags
- **Floating Companion Toolbar**: The minimal control handle (Play/Pause, Stop, Move) hovers unobtrusively over your target game or app.
- **Touch-Passthrough**: The overlay uses `FLAG_NOT_FOCUSABLE` and `FLAG_NOT_TOUCH_MODAL`, ensuring that touches outside the compact toolbar pass through unimpeded to the underlying application.
- **Orientation Awareness**: The overlay independently tracks and stores its window coordinates for Portrait and Landscape modes via `OverlayMenuPositionDataSource`.
- **Live Debug Overlays**: When debug mode is active, the overlay draws bounding boxes around detected conditions without stealing touch focus from your game.

---

### 4. Post Notifications (`PermissionPostNotification`)

Introduced in Android 13 (API 33), runtime notification permission is required for any application that posts status bar notifications.

#### Operational Necessity
1. **Foreground Service Survival**: Under Android's low-memory killer (LMK) policies, background services without an ongoing foreground notification are aggressively terminated within minutes. Macrion runs as an ongoing foreground service (`ForegroundServiceType.MEDIA_PROJECTION`) with an active status bar indicator to guarantee unbroken execution.
2. **Action Notifications**: Scenarios configured with the **Notification** action use this permission to display milestone alerts, completed loop tallies, and critical failure warnings.
3. **Locale / Tasker Fallbacks**: If an external automation tool triggers a scenario while you are actively editing in the app, Macrion posts a fallback notification allowing you to resume when ready.

---

### 5. Battery Optimization Exemption (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`)

Modern mobile operating systems utilize aggressive power-saving engines (e.g., Android Doze mode, Samsung App Power Management, Xiaomi MIUI Battery Saver). When the screen turns off or when an application runs for extended durations without user interaction, the OS throttles CPU wakelocks and suspends background coroutine execution.

By requesting an exemption from battery optimizations:
- Macrion's coroutines remain prioritized during long automation sessions.
- Frame capture timers and periodic interval triggers (`OnTimerReached`) avoid severe clock drift caused by CPU downclocking.

---

## Handling Aggressive OEM Task Killers

Certain device manufacturers implement non-standard process management that can kill Macrion's accessibility service or screen capture pipeline in the background.

If your scenarios abruptly stop or if the accessibility service repeatedly disables itself:

| Manufacturer | Settings Page | Required Configuration |
| :--- | :--- | :--- |
| **Xiaomi / Poco (MIUI / HyperOS)** | App Info ➔ Battery Saver | Set to **No restrictions**. Enable **Autostart**. |
| **Samsung (One UI)** | Settings ➔ Battery ➔ Background usage limits | Add Macrion to **Never sleeping apps**. |
| **OnePlus / Oppo / Realme (ColorOS / OxygenOS)** | App Info ➔ Battery usage | Enable **Allow background activity** and **Allow auto-launch**. |
| **Huawei (EMUI / HarmonyOS)** | Settings ➔ Battery ➔ App launch | Set Macrion to **Manage manually**; enable Auto-launch, Secondary launch, and Run in background. |

---

## Verifying Permission Health in Macrion

Macrion continuously monitors its permission health. If a required system privilege is revoked (e.g., following an OS update or accessibility crash):
1. The app status screen displays a warning badge identifying the missing privilege.
2. Tapping the badge opens the exact Android system settings page directly, eliminating the need to search through nested system menus.
3. All scenario run buttons remain safely disabled until the required permissions are re-confirmed.
