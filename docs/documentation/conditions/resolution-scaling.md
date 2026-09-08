# Resolution & Density Scaling

Android devices span thousands of different screen resolutions, densities (DPI), and aspect ratios—from 1080p phones and 1440p flagships to folding screens and 2.5K tablets. This page explains how Macrion's **`ScalingManager`** optimizes detection performance and ensures touch targets map accurately across physical displays.

---

## The Performance Challenge

Modern flagship devices often feature displays with resolutions of $3200 \times 1440$ pixels or higher. Running OpenCV cross-correlation or neural OCR across multi-megapixel raw frame buffers at 60 FPS can consume substantial CPU power, generate heat, and drain battery life.

To solve this, Macrion uses a two-way resolution scaling pipeline managed by `ScalingManager`:

```text
┌──────────────────────────────────────────────┐
│  Raw Display Frame (e.g. 1440p)              │
└──────────────────────┬───────────────────────┘
                       │ Downscale (scaleDown)
                       ▼
┌──────────────────────────────────────────────┐
│  Detection Buffer (e.g. 1080p / Downscaled)  │
│  • OpenCV Template Matching                  │
│  • Neural OCR Character Recognition          │
└──────────────────────┬───────────────────────┘
                       │ Match Found!
                       │ Upscale (scaleUpDetectionResult)
                       ▼
┌──────────────────────────────────────────────┐
│  Physical Touch Coordinates (Exact 1440p)    │
│  • Dispatched via AccessibilityService       │
└──────────────────────────────────────────────┘
```

---

## How Scaling Operates Under the Hood

### 1. Dynamic Scaling Ratio Calculation
When a scenario starts, `ScalingManager` reads the physical display dimensions from `DisplayConfigManager`:

```kotlin
val displaySize: Point = displayConfigManager.displayConfig.sizePx
val biggestScreenSideSize: Int = max(displaySize.x, displaySize.y)

scalingRatio =
    if (biggestScreenSideSize <= detectionQuality) 1.0
    else detectionQuality / biggestScreenSideSize
```

If the physical display exceeds the configured detection quality ceiling, Macrion computes a fractional `scalingRatio` (e.g., `0.75` or `0.5`).

### 2. Downscaling for Detection (`scaleDown`)
Before running computer vision algorithms:
- The screen framebuffer is downscaled according to `scalingRatio`.
- The condition's template bitmap and search bounding boxes (`detectionArea`) are proportionally downscaled.
- **Result**: Processing load decreases by **$40\% \text{ to } 75\%$**, enabling rapid frame processing with minimal CPU utilization.

### 3. Upscaling for Action Execution (`scaleUp`)
When OpenCV or the OCR engine identifies a matching element:
- The detection coordinate `(x, y)` returned by the detector lives in the downscaled space.
- `ScalingManager.scaleUpDetectionResult()` multiplies the coordinate vector by the inverse scaling ratio.
- The resulting touch coordinates match the device's physical screen pixels, ensuring that subsequent **Click** and **Swipe** actions tap the exact center of the target button.

---

## Screen Rotation & Orientation Handling

When your device flips between **Portrait** and **Landscape**:

1. Android dispatches an orientation change event to the foreground service.
2. `ScalingManager.refreshScaling()` recalculates the active screen width and height.
3. Search bounding boxes and template scaling info are refreshed immediately without needing to restart the scenario.

---

## Cross-Device Scenario Sharing

Because Macrion stores bounding boxes as relative screen coordinates alongside resolution metadata, scenarios exported via [Backup & Sharing](/documentation/migration/backups) can adapt when imported onto devices with different physical screen resolutions.

::: tip Best Practice for Cross-Device Automation
When creating scenarios intended for multiple devices or friends, prefer **[In-Area](/documentation/conditions/condition-image#2-in-area-in-area)** or **[Text OCR](/documentation/conditions/condition-text)** conditions over fixed full-screen templates. Text and localized search boxes adapt naturally across differing display aspect ratios.
:::

---

## Related Documentation

- **[Vision Engine Overview](/documentation/conditions/)** — Real-time frame evaluation pipeline and AND/OR logic.
- **[Image Conditions](/documentation/conditions/condition-image)** — OpenCV template matching modes and threshold configurations.
- **[Backup & Migration](/documentation/migration/backups)** — Exporting and sharing scenarios across devices.
