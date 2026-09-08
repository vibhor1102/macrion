# Counter Triggers

**Counter Triggers** (`TriggerCondition.OnCounterCountReached`) evaluate the value of internal scenario variables against numeric thresholds. They allow automations to track cycle counts, build finite execution loops, and create multi-phase state machines.

---

## Data Structure & Parameters

A Counter Trigger is defined by the following attributes:

| Parameter | Type | Default | Description |
| :--- | :---: | :---: | :--- |
| **Name** | `String` | — | User-defined label (e.g. *"Max Loops Reached"*). |
| **Counter Name** | `String` | — | The identifier of the internal Counter variable to inspect. |
| **Comparison Operator** | `ComparisonOperation` | `GREATER_OR_EQUALS` | The mathematical rule applied to the counter value. |
| **Target Value** | `CounterOperationValue` | — | A static numeric constant **or** the name of another Counter variable. |

---

## Counter State Architecture

Counters are managed in memory by `CountersState`:

1. **Initialization**: When a scenario starts, all declared counters are instantiated with their configured initial default values.
2. **Precision**: Counter values are stored as 64-bit IEEE floating-point numbers (`Double`), supporting whole-number loop tallies (`1, 2, 3...`) as well as fractional values.
3. **Modification**: Actions such as [`ChangeCounter`](/documentation/actions/) can increment, decrement, multiply, set, or reset counter values at any point in the automation.
4. **State Persistence**: Counters exist for the lifetime of the active scenario run. Stopping and restarting the scenario resets all counters back to their default starting values.

---

## Mathematical Comparison Operators

`ConditionsVerifier` evaluates counter rules with double-precision accuracy:

| Operator | Symbol | Evaluation Logic | Example |
| :--- | :---: | :--- | :--- |
| **`GREATER`** | `>` | $\text{Counter} > \text{Target}$ | Trigger Stop when $\text{Errors} > 3$ |
| **`GREATER_OR_EQUALS`** | `≥` | $\text{Counter} \ge \text{Target}$ | Exit Loop when $\text{RoundsCompleted} \ge 10$ |
| **`EQUALS`** | `=` | $|\text{Counter} - \text{Target}| < 10^{-9}$ | Enter Boss Phase when $\text{CurrentWave} = 5$ |
| **`LOWER_OR_EQUALS`** | `≤` | $\text{Counter} \le \text{Target}$ | Trigger Refill when $\text{PotionsLeft} \le 1$ |
| **`LOWER`** | `<` | $\text{Counter} < \text{Target}$ | Alert when $\text{RemainingStamina} < 10$ |

---

## Static Constants vs. Dynamic Counter Operands

The target value on the right-hand side of a comparison rule can be:

### 1. Static Number (`CounterOperationValue.Number`)
Compares against a hardcoded numeric literal:
$$\text{Counter("DungeonRuns")} \ge 25$$
*Example*: Stop the scenario after exactly 25 successful dungeon clears.

### 2. Another Counter Variable (`CounterOperationValue.Counter`)
Compares the counter against a second runtime variable:
$$\text{Counter("CurrentGold")} \ge \text{Counter("TargetGoal")}$$
*Example*: Dynamically compare harvested loot against a user-defined threshold set via an external Tasker variable.

---

## Core Automation Patterns

### Pattern A: Finite Loop Iteration
To run an automation a fixed number of times:
1. Create a counter named `LoopCount` with an initial value of `0`.
2. In your primary event (e.g. "Clear Stage"), attach a **ChangeCounter** action that adds `+1` to `LoopCount`.
3. Create a Trigger Event with a Counter Trigger: `LoopCount >= 10`.
4. Attach a **Stop** action to this trigger event.
5. **Result**: The routine will execute exactly 10 times and then safely shut down.

### Pattern B: Multi-Stage State Machine
Use a counter named `Stage` to sequence complex games:
- `Stage = 0`: Character moves to shop. Once at shop, set `Stage = 1`.
- `Stage = 1`: Buys potions. Once bought, set `Stage = 2`.
- `Stage = 2`: Enters dungeon.
- By gating events with Counter Triggers (`Stage = 0`, `Stage = 1`), events will only evaluate when the automation is in the correct phase.

---

## Related Documentation

- **[Triggers Overview](/documentation/triggers/)** — Overview of all screen-independent trigger primitives.
- **[Timer Triggers](/documentation/triggers/trigger-timer)** — Delays and auto-restarting time intervals.
- **[Counter Actions](/documentation/actions/)** — Incrementing, decrementing, and setting counter values.
