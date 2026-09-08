# Timer Triggers

**Timer Triggers** (`TriggerCondition.OnTimerReached`) execute actions after a specified duration has elapsed. They allow scenarios to implement delayed actions, recurring interval loops, and timeout fallbacks without consuming screen capture resources.

---

## Data Structure & Parameters

A Timer Trigger is defined by the following attributes:

| Parameter | Type | Default | Description |
| :--- | :---: | :---: | :--- |
| **Name** | `String` | — | User-defined label (e.g. *"Refresh Shop Interval"*). |
| **Duration (ms)** | `Long` | `1000 ms` | Elapsed time in milliseconds required before the trigger fires. |
| **Restart When Reached** | `Boolean` | `false` | If `true`, the timer resets and repeats indefinitely; if `false`, it fires once and disables itself. |

---

## How Timer State Works Under the Hood

Timer triggers are managed by `TimersState` within Macrion's processing core:

```text
Event Activated / Scenario Started
             │
             ▼
Calculate Target End Time:
Target = CurrentTime + DurationMs
             │
             ▼
Each Processing Cycle:
Is CurrentTime > Target?
       │               │
       ├─ No ──────────┴── Wait for next cycle
       │
       ▼ Yes
Condition Fulfills!
       │
       ├─ Restart == true  ──► Reset Target = Now + DurationMs (Repeat Loop)
       │
       └─ Restart == false ──► Remove from Active Timers (One-Shot)
```

### 1. One-Shot Delay (`restartWhenReached = false`)
- When the parent event becomes active, Macrion calculates the expiration timestamp:
  $$T_{\text{end}} = T_{\text{start}} + \text{Duration}$$
- The moment $T_{\text{current}} > T_{\text{end}}$, the condition evaluates to fulfilled.
- `TimersState` immediately disables the timer (`setTimerToDisabled`). It will not fire again unless the parent event is explicitly toggled off and back on via a [`ToggleEvent`](/documentation/actions/) action.
- **Use Cases**: Startup delays, allowing game loading screens to settle, or fallback timeouts.

### 2. Periodic Interval Loop (`restartWhenReached = true`)
- When the timer expires, `TimersState` automatically updates the anchor timestamp to current time (`setTimerStartToNow`):
  $$T_{\text{end(next)}} = T_{\text{now}} + \text{Duration}$$
- The timer immediately begins counting down toward the next interval.
- **Use Cases**: Periodic keep-alive taps to prevent phone sleep, casting defensive buffs every 45 seconds, or refreshing a game market every 5 minutes.

---

## Technical Precision & Clock Handling

* **Unified Verification Timestamp**: In `ConditionsVerifier`, all conditions evaluated within a single processing iteration share the exact same reference timestamp (`currentVerificationTsMs`). This ensures multi-condition checks evaluate synchronously without mid-cycle clock drift.
* **Overflow Protection**: Macrion guards against integer overflow when adding large durations to system epoch times:
  ```kotlin
  if (Long.MAX_VALUE - durationMs < startTimeMs) Long.MAX_VALUE
  else startTimeMs + durationMs
  ```

---

## Common Automation Design Patterns

### Pattern A: The Timeout Fallback
Pair an [Image Condition](/documentation/conditions/condition-image) with a one-shot Timer Trigger in an `OR` event:
- **Condition 1**: Match "Victory Screen" image.
- **Condition 2 (`OR`)**: Timer set to 60,000 ms (60 seconds).
- **Result**: If the game completes normally, the victory screen triggers the next step immediately. If the game hangs or lags, the 60-second timer fires anyway, preventing the automation from getting permanently stuck.

### Pattern B: Periodic Anti-Idle Heartbeat
Set up a separate, low-priority Trigger Event:
- **Timer**: 120,000 ms (2 minutes), `restartWhenReached = true`.
- **Action**: Tap an empty, neutral spot on the screen.
- **Result**: Keeps the game connection alive without interrupting the primary farming loop.

---

## Related Documentation

- **[Triggers Overview](/documentation/triggers/)** — Comparison between vision conditions and state triggers.
- **[Counter Triggers](/documentation/triggers/trigger-counter)** — Evaluating internal numeric variables and loop counters.
- **[Dynamic Flow Control](/documentation/actions/)** — Enabling and disabling timer events dynamically with `ToggleEvent`.
