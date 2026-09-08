# Color Conditions

**Color Conditions** (`ScreenCondition.Color`) sample display pixels within a specified screen region and match their RGB values against a target color. Because color evaluations bypass complex computer vision algorithms, they are the fastest condition type in Macrion.

---

## Data Structure & Parameters

A Color Condition is defined by the following attributes:

| Parameter | Type | Default | Description |
| :--- | :---: | :---: | :--- |
| **Name** | `String` | — | User-defined label (e.g. *"Low Health Indicator"*). |
| **Target Color** | `@ColorInt Int` | — | Target RGB color to detect. |
| **Detection Area** | `Rect` | — | Bounding box of the pixel(s) to sample on screen. |
| **Threshold** | `Int` | `90%` | Minimum color similarity percentage (0–100%). |
| **Should Be Detected** | `Boolean` | `true` | `true` for presence detection; `false` for color absence/change. |
| **Priority** | `Int` | `0` | Order of evaluation within the parent Event. |

---

## Single-Pixel vs. Average Area Sampling

Macrion adapts its sampling algorithm depending on the dimensions of your **Detection Area**:

### 1. Single-Pixel Sampling ($1 \times 1$)
- When the detection area is a single point, Macrion samples the exact RGB values of that single coordinate in the framebuffer.
- **Speed**: **Instantaneous ($< 0.1$ ms)**.
- **Use case**: Crisp, solid-color UI indicators, status LEDs, and crisp flat notification badges.

### 2. Area Average Sampling ($W \times H$)
- When the detection area spans multiple pixels, Macrion computes the arithmetic mean across all pixels in the region:
  $$\bar{R} = \frac{1}{N} \sum_{i=1}^N R_i, \quad \bar{G} = \frac{1}{N} \sum_{i=1}^N G_i, \quad \bar{B} = \frac{1}{N} \sum_{i=1}^N B_i$$
- **Speed**: **Sub-millisecond ($< 0.5$ ms)**.
- **Why it matters**: Gradients, textured backgrounds, or semi-transparent health bars naturally fluctuate by a few pixel shades. Area averaging smooths out minor noise and eliminates false positives caused by font sub-pixel rendering.

---

## Distance & Threshold Calculation

Color similarity is evaluated using Euclidean distance across the 3D RGB color spectrum:

$$\text{Distance} = \sqrt{(R_{\text{screen}} - R_{\text{target}})^2 + (G_{\text{screen}} - G_{\text{target}})^2 + (B_{\text{screen}} - B_{\text{target}})^2}$$

The maximum possible Euclidean distance between pure black `(0,0,0)` and pure white `(255,255,255)` is $\sqrt{3 \times 255^2} \approx 441.67$.

Macrion normalizes this distance into a confidence percentage:

$$\text{Confidence Rate} = \left(1 - \frac{\text{Distance}}{441.67}\right) \times 100\%$$

If the resulting confidence meets or exceeds your configured **Threshold**, the condition evaluates as detected.

---

## High-Performance Use Cases

Because Color Conditions execute in less than 0.5 milliseconds, they are ideal for high-frequency automation:

* **Health & Mana Thresholds**: Place a color check at the 25% mark of an in-game health bar. When health drops below 25%, the green/red bar disappears and turns to the dark background color, instantly triggering an auto-heal action.
* **Skill Cooldowns**: Most RPGs desaturate or darken skill icons when on cooldown. Checking for the bright active color tells Macrion exactly when a skill becomes ready to cast.
* **Turn / State Detection**: Check the corner of an end-turn button to determine whether it is your turn (bright yellow) or the opponent's turn (dark gray).

---

## Related Documentation

- **[Vision Engine Overview](/documentation/conditions/)** — Frame verification lifecycle, AND/OR logic, and latency tracking.
- **[Image Conditions](/documentation/conditions/condition-image)** — Detecting complex graphics, icons, and buttons with OpenCV.
- **[Number Conditions](/documentation/conditions/condition-number)** — Reading numeric health or resource values directly off screen.
