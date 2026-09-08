# Vision Engine Overview

Macrion's **Vision Engine** is the real-time computer vision subsystem responsible for inspecting screen contents and determining when automation rules should trigger. This page outlines the internal evaluation pipeline, condition logic operators, match thresholds, and positive versus negative detection.

---

## The Verification Lifecycle

When a Smart Scenario is running, Macrion captures screen frame buffers via `MediaProjection` and passes them through `ConditionsVerifier`.

```text
┌─────────────────────────┐
│ Screen Frame Captured   │
└────────────┬────────────┘
             │
             ▼
┌─────────────────────────┐
│   ConditionsVerifier    │
│   (Evaluates Event)     │
└────────────┬────────────┘
             │
      ┌──────┴──────┐
      ▼             ▼
┌───────────┐ ┌───────────┐
│ AND Logic │ │ OR Logic  │
└─────┬─────┘ └─────┬─────┘
      │             │
      ▼             ▼
  First False   First True
  Immediate     Immediate
  Short-Circuit Short-Circuit
```

### Evaluation Order & Priority
Conditions within an Event are evaluated strictly in **priority order** (from highest priority to lowest). This ordering is critical for performance tuning: placing quick checks (such as single-pixel color tests) before heavy operations (such as full-screen template matching or neural OCR) ensures fast evaluation.

---

## Boolean Operators & Short-Circuit Optimization

Every Event defines a condition operator (`AND` or `OR`) that dictates how its conditions are combined:

### `AND` Operator (All Conditions Must Pass)
- Every condition assigned to the event must evaluate to fulfilled.
- **Short-Circuit Optimization**: The moment any condition fails (`isFulfilled == false`), Macrion immediately aborts further checks for that event loop. Subsequent template matches or OCR scans are skipped entirely, saving valuable CPU cycles.

### `OR` Operator (Any Condition Must Pass)
- The event triggers if at least one condition passes.
- **Short-Circuit Optimization**: The moment any condition succeeds (`isFulfilled == true`), verification halts immediately and execution proceeds directly to the event's action list.

---

## Positive vs. Negative Detection (`shouldBeDetected`)

Every screen condition contains a fundamental boolean parameter: `shouldBeDetected`.

| Detection Mode | `shouldBeDetected` | Fulfill Condition | Common Use Cases |
| :--- | :---: | :--- | :--- |
| **Presence (Default)** | `true` | Element **appears** on screen (`isDetected == true`) | Tapping a "Claim" button, clicking an "OK" prompt, reacting to an alert icon. |
| **Absence (Negative)** | `false` | Element **disappears** from screen (`isDetected == false`) | Waiting for a "Loading..." spinner to vanish before continuing, detecting that an enemy health bar is depleted, confirming a popup has closed. |

Under the hood, `ConditionsVerifier` resolves fulfillment cleanly:

```kotlin
isFulfilled = detectionResult.isDetected == condition.shouldBeDetected
```

---

## Confidence Rates & Match Thresholds

Every visual detector (template matching, color distance, or neural OCR) computes a **confidence rate** between `0%` and `100%`:

- **Threshold Value**: The minimum acceptable similarity percentage configured for that condition.
- **Detection State (`isDetected`)**: Evaluated as `confidenceRate >= threshold`.
- **Tuning Strategy**:
  - **High Threshold (85%–95%)**: High precision, zero false positives. Best for static, crisp interface elements.
  - **Lower Threshold (65%–80%)**: Accommodates minor color grading, transparent backgrounds, or compression artifacts common in 3D mobile games.

---

## Real-Time Latency Monitoring

During scenario execution, `ConditionsVerifier` tracks the exact execution duration of every condition evaluation using nanosecond timestamps (`SystemClock.elapsedRealtimeNanos()`).

These metrics are forwarded to the **Live Debug Panel** HUD on your screen, allowing you to instantly identify which conditions are running fast (sub-millisecond color checks) and which are causing frame latency (unrestricted full-screen template searches).

---

## Screen Condition Types

Explore the detailed technical specifications for each individual condition primitive:

1. **[Image Conditions](/documentation/conditions/condition-image)** — OpenCV normalized cross-correlation, exact area vs. search regions, and cropping best practices.
2. **[Color Conditions](/documentation/conditions/condition-color)** — Single-pixel sampling, average bounding-box color, and Euclidean RGB tolerance.
3. **[Text Conditions (OCR)](/documentation/conditions/condition-text)** — Tencent NCNN neural network inference, supported language alphabets, and fuzzy text matching.
4. **[Number Conditions](/documentation/conditions/condition-number)** — Numeric OCR extraction, mathematical comparison operators (`=`, `≠`, `>`, `<`, `≥`, `≤`), and dynamic counter variables.
5. **[Resolution & Scaling](/documentation/conditions/resolution-scaling)** — How `ScalingManager` dynamically maps coordinates and bitmaps across different screen sizes and aspect ratios.
