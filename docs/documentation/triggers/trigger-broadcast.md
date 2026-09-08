# Broadcast Intent Triggers

**Broadcast Intent Triggers** (`TriggerCondition.OnBroadcastReceived`) listen for incoming Android system broadcasts dispatched by other apps—such as **Tasker**, **MacroDroid**, **Automate**, or desktop **ADB** scripts. 

They allow external automation tools to signal Macrion to take action, switch states, or execute specific routines without user intervention.

---

## Data Structure & Parameters

A Broadcast Intent Trigger is defined by the following attributes:

| Parameter | Type | Default | Description |
| :--- | :---: | :---: | :--- |
| **Name** | `String` | — | User-defined label (e.g. *"Start Boss Fight Broadcast"*). |
| **Intent Action** | `String` | — | The exact Android action string to listen for (e.g. `com.example.macrion.EVENT_START`). |

---

## How It Works Under the Hood

When a scenario containing Broadcast Intent Triggers starts, Macrion's internal `BroadcastsState` registers a dynamic broadcast receiver:

```text
External App (Tasker / ADB)
             │
             ▼ Dispatches Broadcast Intent
Android OS Intent Bus
             │
             ▼ Filter matches "intentAction"
Macrion SafeBroadcastReceiver
             │
             ▼
Sets: broadcastsState[intentAction] = true
             │
             ▼
ConditionsVerifier executes event actions!
             │
             ▼
clearIterationState() resets flag to false
```

1. **Dynamic Registration**: Macrion compiles all `intentAction` strings from your trigger conditions into an Android `IntentFilter` and registers a `SafeBroadcastReceiver`.
2. **Event Triggering**: The moment an intent matching your action string is received, the trigger condition evaluates to fulfilled.
3. **Automatic Reset**: To prevent the event from executing repeatedly in an infinite loop, Macrion automatically clears the received state at the end of the processing iteration (`clearIterationState`).

---

## Triggering via Tasker & Automation Apps

You can send broadcast signals into Macrion from any Android automation app:

### Tasker Setup
1. In your Tasker task, add an action: **System $\to$ Send Intent**.
2. Set **Action** to your custom string (e.g. `com.example.macrion.TRIGGER_FARM`).
3. Set **Cat** (Category) to `None`.
4. Set **Target** to `Broadcast Receiver`.

### MacroDroid / Automate Setup
1. Add a **Send Broadcast** action.
2. Enter the same action string configured in your Macrion Broadcast Trigger.

---

## Triggering via ADB (Command Line)

You can trigger Macrion events directly from a computer or shell script via ADB:

```bash
adb shell am broadcast -a com.example.macrion.TRIGGER_FARM
```

If successful, Android terminal output will report:

```text
Broadcasting: Intent { act=com.example.macrion.TRIGGER_FARM }
Broadcast completed: result=0
```

---

## Best Practices & Action Naming

* **Use Reverse-Domain Action Names**: To prevent accidental conflicts with other apps on your device, always namespace your action strings (e.g., use `com.mygame.farm.START` rather than generic names like `START`).
* **Keep Listeners Active**: The scenario must be loaded and running for the broadcast receiver to catch incoming intents. If you want Tasker to launch a completely stopped scenario from scratch, use Macrion's [Tasker Locale Plugin](/documentation/advanced/external-triggers) instead.

---

## Related Documentation

- **[Triggers Overview](/documentation/triggers/)** — Comparison between vision conditions and state triggers.
- **[Tasker & External Integration](/documentation/advanced/external-triggers)** — Complete guide to Macrion's Tasker action/condition plugins and broadcast APIs.
- **[Intent Actions](/documentation/actions/)** — Sending outbound broadcast intents from Macrion to external apps.
