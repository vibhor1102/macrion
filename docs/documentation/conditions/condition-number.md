# Number Conditions & Logic

**Number Conditions** (`ScreenCondition.Number`) extract numeric values directly from your screen and evaluate them using mathematical comparison operators. They allow automations to make intelligent mathematical decisions based on health points, resource tallies, cooldown timers, or currency counts.

---

## Data Structure & Parameters

A Number Condition is defined by the following attributes:

| Parameter | Type | Default | Description |
| :--- | :---: | :---: | :--- |
| **Name** | `String` | — | User-defined label (e.g. *"Health Below Threshold"*). |
| **Detection Area** | `Rect` | — | Bounding box on screen containing the numbers. |
| **Comparison Operator** | `ComparisonOperation` | `LOWER` | The mathematical rule applied to the screen number. |
| **Comparison Operand** | `CounterOperationValue` | — | A static constant value **or** a dynamic Counter variable. |
| **Number Format** | `NumberFormatType` | `AUTO` | Parsing format (`AUTO`, `INTEGER`, or `DECIMAL`). |
| **Threshold** | `Int` | `80%` | Minimum OCR confidence required to parse digits. |
| **Priority** | `Int` | `0` | Order of evaluation within the parent Event. |

---

## Numeric OCR Engine

While [Text Conditions](/documentation/conditions/condition-text) load full dictionary neural language models, Number Conditions use a streamlined digit classifier constrained to `0–9`, decimal points (`.`), commas (`,`), and negative signs (`-`).

Because the neural search space is reduced to numeric glyphs:
- **Speed**: Evaluates **$3\times$ to $5\times$ faster** than full sentence OCR.
- **Accuracy**: Eliminates character confusion (e.g., prevents confusing the digit `0` with the letter `O`, or `1` with `l`).

---

## Mathematical Comparison Operators

`ConditionsVerifier` evaluates the extracted screen number against your comparison target using the following logic:

| Operator | Symbol | Evaluation Logic | Example |
| :--- | :---: | :--- | :--- |
| **`LOWER`** | `<` | $\text{Screen} < \text{Target}$ | Trigger Heal when $\text{HP} < 250$ |
| **`LOWER_OR_EQUALS`** | `≤` | $\text{Screen} \le \text{Target}$ | Trigger Reload when $\text{Ammo} \le 0$ |
| **`EQUALS`** | `=` | $|\text{Screen} - \text{Target}| < 10^{-9}$ | Trigger Exit when $\text{Wave} = 50$ |
| **`GREATER_OR_EQUALS`** | `≥` | $\text{Screen} \ge \text{Target}$ | Buy Upgrade when $\text{Gold} \ge 15000$ |
| **`GREATER`** | `>` | $\text{Screen} > \text{Target}$ | Halt Farming when $\text{Inventory} > 99$ |

::: tip Floating-Point Equality
Floating-point calculations can suffer from minor rounding variances (e.g., `49.999999` vs `50.0`). Macrion protects against this by using an epsilon threshold ($\epsilon = 10^{-9}$) to ensure mathematical equality checks evaluate reliably.
:::

---

## Static Values vs. Dynamic Counters

The right-hand side of your comparison rule can be set to two different operand types:

### 1. Static Numeric Constant (`CounterOperationValue.Number`)
Compares the screen number against a fixed number you specify:
$$\text{Screen Number} \quad [\text{Operator}] \quad 500.0$$
*Example*: Trigger a heal action whenever on-screen Health drops below `300`.

### 2. Dynamic Counter Variable (`CounterOperationValue.Counter`)
Compares the screen number against an internal variable stored in Macrion's runtime counter state:
$$\text{Screen Number} \quad [\text{Operator}] \quad \text{Counter("MaxHP")} \times 0.3$$
*Example*: Compare current on-screen gold against a counter named `StartingGold` to calculate net session profit and stop farming once a target margin is reached.

---

## Formatting Options (`NumberFormatType`)

- **`AUTO`**: Automatically detects integers or floating-point decimals based on extracted glyphs.
- **`INTEGER`**: Strips decimal points and parses the digits as a whole integer (`1250`).
- **`DECIMAL`**: Normalizes commas and periods to parse floating-point numbers (`12.50`).

---

## Related Documentation

- **[Vision Engine Overview](/documentation/conditions/)** — Frame verification lifecycle, AND/OR logic, and latency tracking.
- **[Text Conditions (OCR)](/documentation/conditions/condition-text)** — Reading alphanumeric words, dialogue, and UI labels.
- **[Counters & Variables](/documentation/actions/)** — How internal counters store and modify variables at runtime.
