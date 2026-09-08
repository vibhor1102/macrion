# Priority & Frame Flow

When a Smart Scenario executes, Macrion captures display frames at high frequency and passes each frame through an evaluation loop. This page explains how **Event Priority** determines evaluation order and how the **`keepDetecting`** property controls whether multiple events can execute on the exact same video frame.

---

## The Frame Processing Loop

Inside `ScenarioProcessor`, each captured display frame undergoes a structured evaluation cycle:

```text
Display Frame Buffer Ingested
             │
             ▼
Filter Active & Ready Events
(Enabled == true && Cooldown Elapsed)
             │
             ▼
Sort by Priority (Descending: 100 → 50 → 0)
             │
             ▼
Evaluate Event Conditions
             │
       ┌─────┴─────────────────────────┐
       ▼ No Match                      ▼ Conditions Fulfilled!
  Move to Next Event              Dispatch Event Actions
                                       │
                                       ▼
                         Is keepDetecting == true?
                                ├── Yes ──► Continue evaluating lower-priority events
                                │           on the SAME frame buffer.
                                │
                                └── No ───► BREAK current frame loop.
                                            Wait for next screen frame.
```

---

## 1. Event Priority System

Every event has an integer **Priority** value (`priority`). Events with higher numerical values are evaluated **first**.

### Why Priority Matters
In any automated workflow, multiple visual conditions may be true at the same time:
* An in-game "Disconnect Alert" might appear over your ongoing "Attack Enemy" loop.
* A "Low Health" warning might appear while navigating a loot menu.

By giving emergency checks a higher priority, you guarantee they intercept the game before ordinary background actions run.

### Recommended Priority Tiering Strategy

| Priority Tier | Value Range | Intended Use Case |
| :--- | :---: | :--- |
| **Emergency Interrupts** | `100+` | Network disconnections, out-of-stamina prompts, low health emergency heals, crash recovery dialogs. |
| **Phase Transitions** | `50 – 99` | "Victory / Stage Complete" banners, "Defeat" screens, reward collection prompts. |
| **Core Gameplay Loop** | `1 – 49` | Skill rotations, character movement, menu navigation, resource gathering. |
| **Idle / Background** | `0` | Periodic anti-idle taps, ambient keep-alive actions. |

---

## 2. The `keepDetecting` Frame-Flow Property

In `ScreenEvent`, the **`keepDetecting`** setting dictates what happens to the remaining events after an event successfully matches.

Under the hood, `ScenarioProcessor` evaluates this single check:

```kotlin
// ScenarioProcessor.kt
executeActions(screenEvent.actions)
if (!screenEvent.keepDetecting) break
```

### Mode A: Break Frame Loop (`keepDetecting = false`, Default)
* **Behavior**: The moment the event matches and dispatches its actions, Macrion immediately **terminates the current frame processing loop**. Lower-priority events are skipped, and Macrion waits for a fresh screen frame.
* **Why It's the Default**: In 95% of touch automations, tapping a button alters the screen (e.g. opens a menu or closes a dialog). Searching the remainder of a stale frame is wasteful and risks acting on pixels that are about to disappear.

### Mode B: Multi-Event Frame Processing (`keepDetecting = true`)
* **Behavior**: When the event matches and dispatches its actions, Macrion **does not break the loop**. It continues evaluating subsequent, lower-priority events **against the very same video frame**.
* **When to Use**:
  - Independent HUD indicators: Checking both an auto-potion trigger and an auto-buff trigger on the exact same frame without waiting for the next capture cycle.
  - Multi-button menus: Clicking two static, non-conflicting buttons that appear simultaneously.

---

## Latency & Short-Circuit Optimization

1. **Place High-Probability Events First**: If your scenario spends 90% of its time on a "Farm Monster" loop, keeping it at a suitable priority level ensures Macrion finds its match immediately on most cycles.
2. **Use Quick Conditions First**: Within any individual event, order conditions so that quick checks (single-pixel Color or exact Area matching) run before heavy operations (neural OCR or full-screen template searches).

---

## Related Documentation

- **[Event Architecture](/documentation/events/)** — Structural hierarchy and domain models.
- **[Event Cooldowns](/documentation/events/cooldowns)** — Throttling re-triggering with post-execution cooldown timers.
- **[Dynamic Flow Control](/documentation/events/dynamic-flow-control)** — State-machine transitions using `ToggleEvent`.
