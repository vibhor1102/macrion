# State Machines & Dynamic Flow Control

In complex mobile games and multi-step apps, different screens often share identical buttons (such as *"OK"*, *"Confirm"*, *"Close"*, or *"Back"*). If every event in your scenario is evaluating simultaneously, Macrion might trigger the wrong action at the wrong time.

This page explains how to build robust **Finite State Machines** using runtime event toggling ([`ToggleEvent`](/documentation/actions/)), ensuring events only evaluate when your automation is in the correct phase.

---

## The Monolithic Event Trap

When all events are enabled all the time:
* **False Positives**: An "OK" button condition meant for claiming daily quests might inadvertently click an "OK" button on a real-money purchase dialog.
* **CPU Waste**: The vision engine must test every template and OCR string on every single frame, even when 80% of those elements cannot possibly exist in the current game phase.

---

## Runtime Flow Control: The `ToggleEvent` Action

The [`ToggleEvent`](/documentation/actions/) action allows an event to dynamically change the enabled state of any other event in the scenario:

| Toggle Mode | Behavioral Effect |
| :--- | :--- |
| **`ENABLED`** | Activates the target event. `ConditionsVerifier` will begin inspecting its conditions on the next frame loop. |
| **`DISABLED`** | Deactivates the target event. `ConditionsVerifier` immediately ceases inspecting it, consuming zero CPU. |
| **`TOGGLE`** | Inverts the target event's current state (active $\to$ dormant, or dormant $\to$ active). |

---

## Architectural Pattern: Phase-Based State Machine

To build reliable multi-step automations, divide your scenario into discrete **Phases**. Only the events belonging to the current phase should be enabled.

```text
┌─────────────────────────┐
│     Phase 1: Lobby      │  ◄── Enabled on Start
│  • Event A: Claim Mail  │
│  • Event B: Enter Fight ├──┐
└─────────────────────────┘  │
                             ▼ When Enter Fight Triggers:
┌─────────────────────────┐  │ • Disables Phase 1 Events
│     Phase 2: Combat     │  │ • Enables Phase 2 Events
│  • Event C: Auto Attack │◄─┘
│  • Event D: Heal        │
│  • Event E: Win Screen  ├──┐
└─────────────────────────┘  │
                             ▼ When Win Screen Triggers:
┌─────────────────────────┐  │ • Disables Phase 2 Events
│     Phase 3: Rewards    │  │ • Enables Phase 3 Events
│  • Event F: Open Chests │◄─┘
│  • Event G: Return Home ├──┐
└─────────────────────────┘  │
                             ▼ When Return Home Triggers:
                             │ • Disables Phase 3 Events
                             └──► Enables Phase 1 Events (Loops!)
```

### Implementing the State Machine in Macrion

1. **Initial Setup**:
   - Set Phase 1 events to `enabledOnStart = true`.
   - Set Phase 2 and Phase 3 events to `enabledOnStart = false` (dormant).
2. **Phase Transitions**:
   - In Event B ("Enter Fight"), attach two `ToggleEvent` actions:
     - `ToggleEvent(Phase 1 Events, DISABLED)`
     - `ToggleEvent(Phase 2 Events, ENABLED)`
3. **Looping Back**:
   - In Event G ("Return Home"), reverse the toggles to re-enable Phase 1.

---

## Graceful Scenario Termination

What happens when an automation routine finishes its work?

### 1. Automatic Termination (All Events Disabled)
Inside `ScenarioProcessor`, the engine continually monitors whether any active tasks remain:

```kotlin
if (processingState.isEveryEventDisabled()) {
    Log.i(TAG, "All events are disabled. Terminating scenario.")
    onStopRequested()
}
```

If your final phase finishes by disabling the last remaining active event, Macrion automatically detects that no further actions can occur and **safely stops execution, releases screen capture tokens, and dismisses the floating overlay.**

### 2. Explicit Termination
You can also terminate immediately by attaching an explicit scenario stop action from any event.

---

## Related Documentation

- **[Event Architecture](/documentation/events/)** — Core domain models and `enabledOnStart` lifecycle.
- **[Priority & Frame Flow](/documentation/events/priority-and-frame-flow)** — Prioritizing phase-transition events over regular gameplay loops.
- **[Event Cooldowns](/documentation/events/cooldowns)** — Throttling triggers to prevent race conditions during transitions.
