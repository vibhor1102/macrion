# Image Conditions

**Image Conditions** (`ScreenCondition.Image`) use computer vision template matching to detect graphical elements, icons, sprites, or buttons on your display. When the target graphic matches above a specified confidence threshold, the condition fulfills.

---

## Data Structure & Parameters

An Image Condition is defined by the following attributes:

| Parameter | Type | Default | Description |
| :--- | :---: | :---: | :--- |
| **Name** | `String` | — | User-defined label (e.g. *"Claim Reward Button"*). |
| **Detection Type** | `Enum` | `AREA` | Scope of the search region: **Exact Area**, **In-Area**, or **Full Screen**. |
| **Threshold** | `Int` | `80%` | Minimum similarity score (0–100%) required to register a match. |
| **Should Be Detected** | `Boolean` | `true` | `true` for presence detection; `false` for absence/disappearance checks. |
| **Priority** | `Int` | `0` | Order of evaluation within the parent Event. |
| **Template Area** | `Rect` | — | The bounding box coordinates `(left, top, right, bottom)` of the cropped template bitmap. |
| **Search Region** | `Rect?` | `null` | The bounding box of the restricted search area (required when Detection Type is `IN_AREA`). |

---

## Search Scope Modes (`DetectionType`)

Macrion supports three search scopes to balance detection flexibility and performance:

### 1. Exact Area (`AREA`)
- **How it works**: Compares the template image *only* against the exact screen coordinates from which it was originally cropped.
- **Performance**: **Ultra-Fast (sub-5ms)**. Because no 2D matrix sliding occurs, OpenCV performs a direct matrix comparison.
- **When to use**: Buttons, menu headers, and HUD elements that never move from their fixed screen position.

### 2. In-Area (`IN_AREA`)
- **How it works**: Restricts the template search to a designated bounding box drawn by the user (`detectionArea`).
- **Performance**: **Fast (5ms–15ms)**. Limits the search space to a localized zone rather than the entire display buffer.
- **When to use**: Items within a scrolling inventory, cards in a hand, health gauges that fluctuate horizontally, or elements contained within a known window.

### 3. Full Screen (`FULL_SCREEN`)
- **How it works**: Scans the entire display framebuffer from top-left to bottom-right.
- **Performance**: **Moderate (20ms–50ms depending on device CPU/GPU)**.
- **When to use**: Randomly appearing collectibles, wandering enemies, or floating popups that can spawn anywhere on screen.

---

## OpenCV Matching Under the Hood

Macrion uses **OpenCV's normalized cross-correlation algorithm (`TM_CCOEFF_NORMED`)** in native C++:

$$\text{Correlation Score} = \frac{\sum (T(x,y) \cdot I(x,y))}{\sqrt{\sum T(x,y)^2 \cdot \sum I(x,y)^2}}$$

1. The captured template bitmap and the screen framebuffer are passed to native OpenCV.
2. The engine computes a correlation matrix across the designated search region.
3. The peak correlation value is converted into a percentage (`0%` to `100%`).
4. If the peak score meets or exceeds your **Threshold**, the condition is marked as detected, and its peak coordinate `(x, y)` is recorded.

::: tip Dynamic Touch Targeting
Because OpenCV returns the exact bounding box and center coordinate of the detected image, subsequent **Click** actions can be configured to tap the dynamic match position rather than a static coordinate.
:::

---

## Cropping Best Practices

How you crop your template image directly impacts recognition reliability and CPU usage:

* **Crop the Distinctive Core**: Avoid including large generic backgrounds, flat gray borders, or transparent edges. Focus tightly on high-contrast text or unique icons.
* **Exclude Animated Effects**: Many mobile games add pulsating glows, shimmering particle effects, or breathing animations to buttons. Crop the static interior of the button rather than the glowing border.
* **Keep Templates Compact**: Smaller templates evaluate exponentially faster. A $60 \times 60$ pixel icon requires significantly fewer matrix multiplication operations than a $500 \times 300$ pixel popup window.

---

## Related Documentation

- **[Vision Engine Overview](/documentation/conditions/)** — Frame verification lifecycle, AND/OR logic, and latency tracking.
- **[Color Conditions](/documentation/conditions/condition-color)** — Ultra-fast RGB color sampling for health bars and indicators.
- **[Resolution & Scaling](/documentation/conditions/resolution-scaling)** — How template bitmaps adapt when running across different screen resolutions.
