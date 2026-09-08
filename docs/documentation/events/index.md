# Event Architecture

In Macrion, an **Event** is the foundational building block of smart automation. It represents an atomic cause-and-effect rule: *"When specific conditions are met on screen or in system state, execute a designated sequence of actions."*

This page documents the structural hierarchy, data models, event types, and lifecycle states that govern how events operate.

---

## The Automation Hierarchy

Every Macrion smart automation routine is structured in a clean three-tier hierarchy:

```text
┌──────────────────────────────────────────────────────────┐
│                       SCENARIO                           │
│  • Global Counter Variables                              │
│  • General Scenario Settings & Randomization Toggles     │
└────────────────────────────┬─────────────────────────────┘
                             │ Contains 1 or more
                             ▼
┌──────────────────────────────────────────────────────────┐
│                        EVENT                             │
│  • Boolean Operator (AND / OR)                           │
│  • Execution Priority                                    │
│  • Cooldown Timer (cooldownMs)                           │
│  • Frame Flow Property (keepDetecting)                   │
│  • Initial State (enabledOnStart)                        │
└──────────────┬───────────────────────────┬───────────────┘
               │ Evaluates                 │ Dispatches
               ▼                           ▼
┌──────────────────────────────┐ ┌─────────────────────────┐
│         CONDITIONS           │ │         ACTIONS         │
│  • Image Template Matching   │ │  • Clicks & Swipes      │
│  • Color Sampling            │ │  • Text Injection       │
│  • Neural OCR (Text/Numbers) │ │  • Counter Math         │
│  • Timers / Counters         │ │  • Event Toggles        │
│  • Broadcast Intents         │ │  • Broadcast Intents    │
└──────────────────────────────┘ └─────────────────────────┘
```

---

## Event Types: Screen Events vs. Trigger Events

Macrion splits events into two specialized domain classes:

| Attribute | Screen Event (`ScreenEvent`) | Trigger Event (`TriggerEvent`) |
| :--- | :--- | :--- |
| **Condition Types** | [Image](/documentation/conditions/condition-image), [Color](/documentation/conditions/condition-color), [Text](/documentation/conditions/condition-text), [Number](/documentation/conditions/condition-number) | [Timer](/documentation/triggers/trigger-timer), [Counter](/documentation/triggers/trigger-counter), [Broadcast Intent](/documentation/triggers/trigger-broadcast) |
| **Inspection Target** | Display frame buffers captured via `MediaProjection`. | System clock, memory variables, and Android intent bus. |
| **Frame Flow (`keepDetecting`)** | Supported. Dictates whether matching halts or continues on the current frame. | Not applicable (triggers execute on event loop intervals). |
| **Cooldown (`cooldownMs`)** | Supported. Enforces post-execution refractory period. | Handled via timer intervals and counter math. |

---

## Event Lifecycle & Initial State (`enabledOnStart`)

Events are not required to be active all the time. Every event specifies an **`enabledOnStart`** boolean property:

* **`enabledOnStart = true` (Default)**: The event is active immediately upon launching the scenario. `ConditionsVerifier` will inspect its conditions on every frame.
* **`enabledOnStart = false` (Dormant)**: The event begins in a sleeping state. It consumes zero CPU cycles and its conditions are completely ignored until another event awakens it at runtime using a [`ToggleEvent`](/documentation/actions/) action.

::: tip Why Dormant Events Matter
Setting secondary phases to `enabledOnStart = false` prevents race conditions and false positives. For example, your *"Loot Boss Chest"* event shouldn't even be evaluating while your character is still in the *"Town Lobby"* phase.
:::

---

## Validation & Completeness Rules (`isComplete()`)

To ensure scenario integrity and prevent runtime crashes, Macrion enforces strict validation rules before an event can be saved to the local Room database:

1. **Non-Blank Name**: The event must have a non-empty name identifier.
2. **At Least One Condition**: The event must define at least one complete condition.
3. **At Least One Action**: The event must define at least one complete action.
4. **Target Reference Integrity**: If an action is configured to click on the dynamic coordinates of a matched image (`clickOnConditionId`), that referenced condition must exist and belong to the same event.

---

## Related Documentation

- **[Priority & Frame Flow](/documentation/events/priority-and-frame-flow)** — How evaluation order and `keepDetecting` govern single-frame multi-event execution.
- **[Event Cooldowns](/documentation/events/cooldowns)** — Preventing double-clicks and throttling execution with `cooldownMs`.
- **[State Machines & Flow Control](/documentation/events/dynamic-flow-control)** — Transitioning between workflow phases using `ToggleEvent`.
