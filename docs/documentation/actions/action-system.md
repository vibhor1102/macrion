# System Actions: Navigation & App Control

**System Actions** provide coordinate-independent, hardware-level control over Android's core navigation primitives. By communicating directly with Android's Accessibility framework through `AccessibilityService.performGlobalAction`, Macrion can execute system-wide commands—such as navigating back, returning to the launcher home screen, or opening the multi-tasking app switcher—without touching the physical display or relying on coordinate-dependent visual buttons.

---

## Supported System Actions

Macrion implements three primary global actions:

```
SystemAction.Type
 ├── BACK        -> AccessibilityService.GLOBAL_ACTION_BACK
 ├── HOME        -> AccessibilityService.GLOBAL_ACTION_HOME
 └── RECENT_APPS -> AccessibilityService.GLOBAL_ACTION_RECENTS
```

```kotlin
// ActionExecutor.kt - Dispatching system actions
private suspend fun executeSystemAction(action: SystemAction) {
    val globalAction = when (action.type) {
        SystemAction.Type.BACK -> AccessibilityService.GLOBAL_ACTION_BACK
        SystemAction.Type.HOME -> AccessibilityService.GLOBAL_ACTION_HOME
        SystemAction.Type.RECENT_APPS -> AccessibilityService.GLOBAL_ACTION_RECENTS
    }

    withContext(Dispatchers.Main) {
        androidExecutor.performGlobalAction(globalAction)
    }
}
```

---

## Action Reference

### 1. Back (`BACK`)

Simulates pressing the physical Android Back button or performing the system Back edge-swipe gesture.

- **Underlying Constant**: `AccessibilityService.GLOBAL_ACTION_BACK`
- **Key Use Cases**:
  - **Dismissing In-Game Popups & Ads**: Many reward advertisements, daily login banners, and rate-us prompts can be dismissed instantly with a Back press, eliminating the need to locate tiny, moving "X" close buttons.
  - **Exiting Menus & Sub-Screens**: Navigating up out of nested menus, settings pages, or character sheets back to the main lobby.
  - **Dismissing Keyboards & Dropdowns**: Closes the soft keyboard without submitting form changes.

> [!TIP]
> Using `BACK` rather than tapping an on-screen "X" button ensures your scenario functions reliably across different aspect ratios and device models where close buttons move.

### 2. Home (`HOME`)

Directly returns the operating system to the primary home screen launcher.

- **Underlying Constant**: `AccessibilityService.GLOBAL_ACTION_HOME`
- **Key Use Cases**:
  - **Scenario Completion**: Minimizing the active game or automation task once all cycles have completed.
  - **Emergency Fallback**: Returning to a clean launcher state if an unexpected application crash or deep freeze occurs.
  - **Multi-App Pipelines**: Returning to the home launcher before initiating a secondary application flow.

### 3. Recent Apps (`RECENT_APPS`)

Opens the Android system Recent Apps (Overview / Multi-Tasking) screen.

- **Underlying Constant**: `AccessibilityService.GLOBAL_ACTION_RECENTS`
- **Key Use Cases**:
  - **App Switching**: Accessing recently opened applications to switch contexts.
  - **Memory Reset & Task Clearing**: Triggering task-card swipe gestures to force-close an unresponsive target application and reload it from a fresh state.

---

## Technical Advantages Over Gesture Emulation

Automating navigation using `SystemAction` offers several decisive advantages over gesture emulation (e.g., swiping inward from the screen edge):

| Feature | `SystemAction` | Gesture Emulation (Edge Swipe / Tap) |
| :--- | :--- | :--- |
| **Resolution Independence** | 100% independent. Works identically on phones, tablets, and foldables. | Dependent on screen width, display density, and gesture margin widths. |
| **Execution Latency** | Sub-millisecond direct IPC call to Android's window manager. | Requires full gesture duration (typically `150 ms` to `300 ms`). |
| **Reliability** | Native framework dispatch; immune to foreground app gesture interception. | Can be blocked or misclassified by games capturing edge touches. |
| **Orientation Safety** | Works identically in Portrait and Landscape orientations. | Edge swipe coordinates flip axes between Portrait and Landscape modes. |

---

## Action Parameters

| Parameter | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| **`type`** | SystemAction.Type | Yes | One of `BACK`, `HOME`, or `RECENT_APPS`. |
| **`name`** | String | Optional | Descriptive label for display within the scenario action list (e.g., "Dismiss Promo Popup"). |
| **`priority`** | Integer | Yes | The execution sequence index among sibling actions within the parent event. |
