# Event Triggers Overview

While [Screen Conditions](/documentation/conditions/) inspect visual display pixels using computer vision, **Event Triggers** (`TriggerCondition` / `TriggerEvent`) evaluate internal application state, time intervals, and external Android system events. 

Triggers operate independently of screen contents, requiring zero GPU rendering or OpenCV image processing.

---

## Screen Conditions vs. Event Triggers

Understanding when to use a visual condition versus an event trigger is fundamental to designing clean, battery-efficient automations:

| Dimension | Screen Conditions (`ScreenCondition`) | Event Triggers (`TriggerCondition`) |
| :--- | :--- | :--- |
| **Inspection Target** | Display frame buffers, pixels, textures, and text. | System clock timestamps, internal variables, and Android Intents. |
| **Hardware Resources** | Requires `MediaProjection`, CPU/GPU for OpenCV & neural OCR. | Pure state comparisons in memory ($< 0.1$ ms evaluation). |
| **Screen Dependency** | Requires the target app to be open and rendering on screen. | Can fire regardless of what app or screen is currently active. |
| **Primary Use Cases** | Reacting to buttons, health bars, dialogue, and popups. | Loop counters, periodic heartbeats, wait timers, and Tasker IPC. |

---

## The TriggerEvent Architecture

In Macrion's domain model, an automation routine is organized into **Events**:

- **Screen Events (`ScreenEvent`)**: Contain one or more visual conditions (Image, Color, Text, Number).
- **Trigger Events (`TriggerEvent`)**: Contain one or more trigger conditions (Timer, Counter, Broadcast).

### Shared Event Capabilities
Trigger Events inherit the full capabilities of Macrion's event pipeline:
* **Initial State (`enabledOnStart`)**: Can be active immediately when the scenario launches, or start in a disabled state until activated dynamically by a [`ToggleEvent`](/documentation/actions/) action.
* **Boolean Operators (`AND` / `OR`)**: Combine multiple triggers within a single event (e.g. fire when `Timer Reached` **AND** `Counter > 5`).
* **Execution Priority**: Dictates execution order relative to other events.
* **Event Cooldowns**: Set minimum elapsed time before the trigger can execute again, preventing accidental re-triggering.

---

## Performance & Battery Benefits

Because Trigger Events do not capture or process video frames:
1. **Idle Efficiency**: Scenarios waiting on a timer or external broadcast consume negligible CPU cycles and preserve battery life during long standby sessions.
2. **Deterministic Speed**: Evaluating a counter or timer comparison takes less than a microsecond, eliminating frame-capture latency.

---

## Available Trigger Types

Explore the detailed specifications for each trigger condition:

1. **[Timer Triggers](/documentation/triggers/trigger-timer)** — Fixed delays, one-shot startup timers, and auto-restarting interval loops.
2. **[Counter Triggers](/documentation/triggers/trigger-counter)** — Evaluating internal variables against numeric constants or other counters (`=`, `≠`, `>`, `<`, `≥`, `≤`).
3. **[Broadcast Intent Triggers](/documentation/triggers/trigger-broadcast)** — Listening for incoming system broadcasts dispatched by Tasker, Automate, or ADB.
