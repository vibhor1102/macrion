# Touch Actions: Clicks & Swipes

Touch gestures form the primary mechanism through which Macrion interacts with target applications. Injected via Android's `AccessibilityService.dispatchGesture` API, Macrion's touch subsystem can simulate instantaneous taps, sustained long-presses, continuous vector swipes, and dynamic tracking clicks that follow moving targets on screen.

---

## Clicks

A **Click** action injects a single touch stroke at a designated point on the display.

```
Click Action Parameters
 ├── Position Mode: USER_SELECTED (Static) vs ON_DETECTED_CONDITION (Dynamic)
 ├── Click Offset: (dx, dy) in pixels (Condition-relative only)
 └── Press Duration: Time held down in milliseconds (1 ms to 65,000 ms)
```

### Position Types

Macrion provides two distinct modes for determining the click coordinates:

#### 1. User Selected (`USER_SELECTED`)
The click lands at a fixed, absolute display coordinate `Point(x, y)` chosen manually during scenario authoring.
- **When to Use**: For static UI elements that never move (e.g., bottom navigation bars, top-corner settings gears, fixed "OK" dialog buttons).
- **Coordinate System**: Absolute screen pixels relative to the current display orientation (Portrait or Landscape).

#### 2. On Detected Condition (`ON_DETECTED_CONDITION`)
Instead of tapping a hardcoded coordinate, Macrion dynamically targets the center of an image, color patch, or text phrase identified during the current frame's vision evaluation.

How the target coordinate is resolved depends on the parent event's condition logic operator:
- **`OR` Logic**: When an event evaluates with `OR` logic, multiple conditions may be checked. Macrion automatically selects the first condition that matched in the frame via [`getFirstScreenConditionDetectedResult()`](https://github.com/vibhor1102/Macrion/blob/main/core/smart/processing/src/main/java/io/github/vibhor1102/macrion/core/processing/data/processor/ActionExecutor.kt#L138).
- **`AND` Logic**: When all conditions must match simultaneously, you explicitly configure which specific condition to tap via the `clickOnConditionId` field.

```kotlin
// ActionExecutor.kt - Dynamic click position resolution
private fun getOnConditionClickPath(event: Event, click: Click, results: ConditionsResults?): Path? {
    if (event !is ScreenEvent) return null

    val result = when {
        event.conditionOperator == OR -> results?.getFirstScreenConditionDetectedResult()
        click.clickOnConditionId != null -> results?.getScreenConditionResult(click.clickOnConditionId!!.databaseId)
        else -> null
    } ?: return null

    return Path().apply {
        moveTo(
            position = Point(
                (result.position?.x ?: 0) + (click.clickOffset?.x ?: 0),
                (result.position?.y ?: 0) + (click.clickOffset?.y ?: 0),
            ),
            random = random,
        )
    }
}
```

### Dynamic Targeting: Click Offset

Often, the visual indicator you detect is not the exact element you need to tap. For example:
- A text label beside a checkbox or radio button.
- A health bar above an enemy unit in a mobile game.
- A numerical price label next to a "Buy" button.

The **Click Offset** (`Point(dx, dy)`) adds a relative pixel translation to the detected condition center:

$$\text{TouchX} = \text{ConditionX} + dx$$
$$\text{TouchY} = \text{ConditionY} + dy$$

| Target Scenario | Offset Example | Behavior |
| :--- | :--- | :--- |
| **Direct Center** | `(0, 0)` | Taps the exact center of the detected bounding box. |
| **Right Adjacent Button** | `(+120, 0)` | Detects an item description and taps a "Claim" button 120 pixels to its right. |
| **Underneath Input Box** | `(0, +45)` | Detects a field label ("Password:") and taps into the text field 45 pixels below it. |

---

## Press Duration: Taps vs. Long-Presses

The `pressDuration` parameter defines the duration in milliseconds between the finger contact (`ACTION_DOWN`) and release (`ACTION_UP`):

$$\text{Duration} \in [1\text{ ms}, 65\,000\text{ ms}]$$

### Standard Tap (Short Press)
- **Recommended Range**: `30 ms` to `60 ms` (default: `50 ms`).
- **Characteristics**: Instantaneous touch down and up. Recognized by standard Android buttons, list items, and touch targets without triggering long-click context menus.

### Long-Press (Sustained Touch)
- **Recommended Range**: `500 ms` to `2000 ms`.
- **Characteristics**: Holds the finger steady at the designated position before lifting. Used for opening context menus, selecting multi-item rows, or charging abilities in games.

> [!WARNING]
> Setting press durations below `15 ms` may cause certain gaming engines (such as Unity or Unreal engine ports) or high-refresh-rate touch digitizers to miss the stroke entirely due to frame polling intervals.

---

## Swipes & Drags

A **Swipe** action performs a continuous, single-stroke gesture moving from an origin coordinate (`from`) to a destination coordinate (`to`) across a defined duration (`swipeDuration`).

```kotlin
// GestureHelpers.kt - Constructing a linear swipe vector
fun Path.line(from: Point?, to: Point?, random: Random?) {
    if (from == null || to == null) return
    moveTo(from, random)
    lineTo(to, random)
}
```

### Motion Types by Duration

The travel duration dictates the physical velocity and resulting Android gesture interpretation:

| Swipe Duration | Gesture Type | Description & Typical Use |
| :--- | :--- | :--- |
| **`50 ms` – `150 ms`** | **Fling / Flick** | High-velocity flick that imparts significant inertial scrolling momentum. Ideal for rapidly skipping through long lists or feeds. |
| **`300 ms` – `600 ms`** | **Standard Scroll** | Controlled page or list scroll with minimal inertia. Ideal for moving forward by exactly one screen height or carousel card. |
| **`1000 ms` – `3000 ms`** | **Precision Drag** | Slow, deliberate displacement. Holds touch contact so elements can be dragged across the screen (e.g., dragging items into inventory slots or moving puzzle pieces). |

### Directional Vectors

Swipes are defined by 2D vector mathematics:

```
          (X, Y_top)
              ▲
              │   Upward Swipe (Scroll Down)
              │   from: (X, Y_bottom) -> to: (X, Y_top)
              │
(X_left, Y) ──┼──▶ (X_right, Y)
              │   Rightward Swipe (Page Left)
              │   from: (X_left, Y) -> to: (X_right, Y)
              ▼
         (X, Y_bottom)
```

---

## Resolution Scaling & Touch Accuracy

When screen conditions evaluate at downscaled resolutions for performance (e.g., processing frames downscaled to `640x360`), detection coordinates must be translated back into physical device coordinates for touch dispatch.

Macrion's [`ScalingManager`](https://github.com/vibhor1102/Macrion/blob/main/core/smart/processing/src/main/java/io/github/vibhor1102/macrion/core/processing/data/scaling/ScalingManager.kt) ensures pixel-perfect touch mapping:

```
[Camera / Screen Capture: 2400 x 1080]
              │
      scaleDown(factor)
              ▼
[Vision Engine Detection: 1200 x 540]
              │
  Image matched at center: (600, 270)
              │
    scaleUpDetectionResult(factor)
              ▼
[Physical Gesture Dispatch: (1200, 540)]
```

Physical gestures are always dispatched against native device display coordinates, regardless of the internal downscaling factor used during image matching.

---

## Gesture Randomization in Detail

When **Randomization** is enabled, both Clicks and Swipes undergo spatial perturbation:
- Every coordinate $(x, y)$ is adjusted by a random offset $\Delta \in [-5, +5]$ pixels via `Random.nextIntInOffset()`.
- For swipes, both the `from` and `to` points receive independent offsets, introducing subtle directional jitter similar to real human finger travel.
- Press and swipe durations are perturbed by $\Delta t \in [-5, +5]$ milliseconds.

This guarantees that bot detection algorithms tracking identical repetitive touch centroids cannot identify a fixed coordinate signature.
