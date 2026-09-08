# Event Cooldowns

In graphical user interfaces and mobile games, buttons and status indicators rarely vanish instantaneously when tapped. They often play click animations, fade-out effects, or screen transitions that take between 200 and 1,000 milliseconds.

This page explains how **Event Cooldowns** (`cooldownMs`) prevent duplicate touches, throttle execution, and synchronize automations with game animation timings.

---

## The UI Animation Problem

Consider an event designed to tap an in-game "Claim Reward" button:

1. **Frame 1 (0 ms)**: Macrion detects the "Claim" button and dispatches a tap.
2. **Frame 2 (33 ms)**: The game receives the tap and begins a fade-out animation. The button is still visible on screen.
3. **Frame 3 (66 ms)**: The button is still 50% visible during the fade.

Without a cooldown, Macrion would detect the button again on Frame 2, Frame 3, and Frame 4, firing 4 rapid taps on a button intended to be pressed only once.

---

## How Cooldowns Work Under the Hood

Every `ScreenEvent` specifies a **Cooldown Duration** in milliseconds (`cooldownMs`). 

Cooldowns are tracked in memory by `CooldownsState`:

```text
Event Actions Dispatched!
            │
            ▼
Set Cooldown Timestamp:
CooldownEnd = CurrentTime + CooldownMs
            │
            ▼
Subsequent Frame Ingestion:
Is CurrentTime < CooldownEnd?
       │                    │
       ├─ Yes (On Cooldown) ┴──► Skip Condition Evaluation!
       │                         (Consumes zero CPU)
       │
       └─ No (Ready) ──────────► Evaluate Conditions Normally
```

1. **Cooldown Stamping**: The moment an event's actions are dispatched, `CooldownsState` records an expiration timestamp:
   $$T_{\text{end}} = T_{\text{now}} + \text{CooldownMs}$$
2. **Fast Pre-Filter**: During subsequent frame loops, `ScenarioProcessor` checks if the event is currently on cooldown. If so, its conditions are **skipped entirely**, avoiding redundant template matching or OCR processing.
3. **Independent Per-Event Tracking**: Cooldowns are tracked individually per event ID. An event on a 10-second cooldown has no effect on other events, which continue evaluating and firing independently.

---

## Recommended Cooldown Values

| Automation Scenario | Recommended Cooldown | Why |
| :--- | :---: | :--- |
| **Menu Buttons & Dialogs** | `800 ms – 1,500 ms` | Provides ample time for screen transition animations, network loading spinners, and modal dismissal. |
| **Consumable Potions / Items** | `2,000 ms – 5,000 ms` | Prevents drinking multiple health potions when a single potion is sufficient to replenish the health bar. |
| **In-Game Skill / Spell Rotations** | Match skill cooldown (e.g. `8,000 ms`) | Synchronizes Macrion with the game's actual spell recharge timers. |
| **Rapid Fire / Ore Mining** | `0 ms` (No cooldown) | Dispatches actions as rapidly as frame capture and accessibility gesture injection allow. |

---

## Cooldowns vs. Action Pauses

It is important to distinguish between **Event Cooldowns** and **Action Pauses**:

* **Action Pause (`Pause`)**: Halts execution *inside* the event's action list (e.g. tap button A, wait 500 ms, tap button B). The entire scenario blocks while waiting.
* **Event Cooldown (`cooldownMs`)**: Non-blocking. Once actions finish, only that specific event is placed on a refractory period. Other active events continue evaluating and triggering freely.

---

## Related Documentation

- **[Event Architecture](/documentation/events/)** — Structural hierarchy and domain models.
- **[Priority & Frame Flow](/documentation/events/priority-and-frame-flow)** — Evaluation order and frame flow control.
- **[Dynamic Flow Control](/documentation/events/dynamic-flow-control)** — Transitioning between workflow phases using `ToggleEvent`.
