# Text Conditions & Neural OCR

**Text Conditions** (`ScreenCondition.Text`) read written words and phrases directly off your screen using an embedded deep neural network. They allow your automations to react dynamically to dialogue, player names, button labels, and system status messages.

---

## Data Structure & Parameters

A Text Condition is defined by the following attributes:

| Parameter | Type | Default | Description |
| :--- | :---: | :---: | :--- |
| **Name** | `String` | — | User-defined label (e.g. *"Defeat Victory Message"*). |
| **Target Text** | `String` | — | The exact string or phrase to detect on screen. |
| **Alphabet** | `OCRAlphabet` | `LATIN` | Target character set / language model. |
| **Detection Area** | `Rect` | — | The bounding box on screen where text is expected to appear. |
| **Threshold** | `Int` | `80%` | Minimum fuzzy string similarity required to trigger a match. |
| **Should Be Detected** | `Boolean` | `true` | `true` for presence detection; `false` for text disappearance. |
| **Priority** | `Int` | `0` | Order of evaluation within the parent Event. |

---

## Under the Hood: Tencent NCNN Neural Inference

Unlike legacy OCR engines that rely on slow, monolithic engines, Macrion embeds **Tencent's NCNN neural inference framework**, hand-optimized in C++ for mobile ARM NEON architectures.

### The Two-Stage Neural Pipeline

1. **Text Detection Model (`det.ncnn`)**: Scans the designated detection area to locate text baselines, character clusters, and word contours.
2. **Text Recognition Model (`rec.ncnn`)**: Feeds extracted word polygons into a lightweight convolutional-recurrent neural network that maps visual features to Unicode characters.

All neural inference executes **100% offline and on-device**. No screen crops or text strings are ever transmitted over the network.

---

## Supported Language Alphabets (`OCRAlphabet`)

Macrion provides dedicated neural recognition models tailored for diverse writing systems:

| Alphabet | Supported Scripts & Languages |
| :--- | :--- |
| **Latin** | English, Spanish, French, German, Portuguese, Italian, Vietnamese, and all Latin-based alphabets. |
| **Chinese (Simplified)** | Simplified Chinese characters ($\approx 6,000+$ glyph dictionary). |
| **Chinese (Traditional)** | Traditional Chinese characters used in Taiwan, Hong Kong, and classic games. |
| **Japanese** | Kanji, Hiragana, and Katakana scripts. |
| **Korean** | Hangul syllables and common glyphs. |
| **Cyrillic** | Russian, Ukrainian, Belarusian, Bulgarian, Serbian. |
| **Devanagari** | Hindi, Marathi, Sanskrit, Nepali. |
| **Arabic** | Arabic, Persian/Farsi, and Urdu right-to-left scripts. |
| **Dravidian Alphabets** | Dedicated models for **Kannada**, **Tamil**, and **Telugu**. |

---

## Fuzzy String Matching & Thresholds

Mobile game text is rarely rendered with pure black-and-white pixel crispness: text is often anti-aliased, drop-shadowed, or rendered across fluctuating 3D backgrounds.

To ensure reliable matching despite rendering noise, Macrion calculates character-level similarity:

$$\text{Similarity Score} = \left(1 - \frac{\text{Levenshtein Distance}}{\max(\text{Length}_{\text{target}}, \text{Length}_{\text{screen}})}\right) \times 100\%$$

- **Threshold (Default: 80%)**: Allows the condition to fulfill even if a drop shadow causes a character substitution (such as detecting `"C1aim"` instead of `"Claim"`).
- **Threshold (100%)**: Enforces strict character-for-character exact matching.

---

## Optimization Best Practices

* **Confine the Detection Area**: Neural OCR is more computationally demanding than simple template matching. Never set a text detection area to the entire screen; draw the bounding box tightly around the subtitle banner, dialogue box, or button label.
* **Match the Target Alphabet**: Always select the alphabet that matches your target game language. Running the Latin model over Japanese Kanji will result in decoding errors and failed conditions.

---

## Related Documentation

- **[Vision Engine Overview](/documentation/conditions/)** — Frame verification lifecycle, AND/OR logic, and latency tracking.
- **[Number Conditions](/documentation/conditions/condition-number)** — Specialized, ultra-fast numeric OCR with mathematical comparison operators.
- **[Image Conditions](/documentation/conditions/condition-image)** — OpenCV graphic matching for icons and graphical sprites.
