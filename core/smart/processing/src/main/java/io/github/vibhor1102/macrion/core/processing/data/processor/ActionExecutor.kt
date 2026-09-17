/*
 * Copyright (C) 2024 Kevin Buzeau
 * Copyright (C) 2026 Vibhor Goel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.vibhor1102.macrion.core.processing.data.processor

import io.github.vibhor1102.macrion.core.base.crash.CrashDiagnostics

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent as AndroidIntent
import android.graphics.Path
import android.graphics.Point
import android.util.Log

import io.github.vibhor1102.macrion.core.base.workarounds.UnblockGestureScheduler
import io.github.vibhor1102.macrion.core.base.workarounds.buildUnblockGesture
import io.github.vibhor1102.macrion.core.common.actions.AndroidActionExecutor
import io.github.vibhor1102.macrion.core.common.actions.gesture.buildSingleStroke
import io.github.vibhor1102.macrion.core.common.actions.gesture.line
import io.github.vibhor1102.macrion.core.common.actions.gesture.moveTo
import io.github.vibhor1102.macrion.core.common.actions.model.ActionNotificationRequest
import io.github.vibhor1102.macrion.core.common.actions.text.findCounterReferences
import io.github.vibhor1102.macrion.core.common.actions.text.replaceCounterReferences
import io.github.vibhor1102.macrion.core.common.actions.utils.getPauseDurationMs
import io.github.vibhor1102.macrion.core.domain.model.counter.CounterOperationValue
import io.github.vibhor1102.macrion.core.domain.model.OR
import io.github.vibhor1102.macrion.core.domain.model.action.Intent
import io.github.vibhor1102.macrion.core.domain.model.action.Click
import io.github.vibhor1102.macrion.core.domain.model.action.Pause
import io.github.vibhor1102.macrion.core.domain.model.action.Swipe
import io.github.vibhor1102.macrion.core.domain.model.action.ToggleEvent
import io.github.vibhor1102.macrion.core.domain.model.action.ChangeCounter
import io.github.vibhor1102.macrion.core.domain.model.action.Notification
import io.github.vibhor1102.macrion.core.domain.model.action.SetText
import io.github.vibhor1102.macrion.core.domain.model.action.SystemAction
import io.github.vibhor1102.macrion.core.domain.model.action.intent.putDomainExtra
import io.github.vibhor1102.macrion.core.domain.model.event.Event
import io.github.vibhor1102.macrion.core.domain.model.event.ScreenEvent
import io.github.vibhor1102.macrion.core.processing.data.processor.state.ProcessingState
import io.github.vibhor1102.macrion.core.domain.model.action.ExternalAction
import io.github.vibhor1102.macrion.core.domain.model.action.PlaySound
import io.github.vibhor1102.macrion.core.domain.model.action.CaptureScreenshot

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * Execute the actions of an event.
 *
 * @param androidExecutor the executor for the actions requiring an interaction with Android.
 * @param processingState the state of the current processing (counters, enabled events...).
 * @param randomize true to randomize the actions values a bit (positions, timers...), false to be precise.
 */
internal class ActionExecutor(
    private val androidExecutor: AndroidActionExecutor,
    private val processingState: ProcessingState,
    randomize: Boolean,
    unblockWorkaroundEnabled: Boolean = false,
) {

    init { androidExecutor.resetState() }

    private val random: Random? =
        if (randomize) Random(System.currentTimeMillis()) else null

    private val unblockGestureScheduler: UnblockGestureScheduler? =
        if (unblockWorkaroundEnabled) UnblockGestureScheduler()
        else null


    suspend fun onScenarioLoopFinished() {
        if (unblockGestureScheduler?.shouldTrigger() == true) {
            withContext(Dispatchers.Main) {
                Log.i(TAG, "Injecting unblock gesture")
                androidExecutor.dispatchGesture(
                    GestureDescription.Builder().buildUnblockGesture()
                )
            }
        }
    }

    suspend fun executeActions(event: Event, results: ConditionsResults? = null) {
        event.actions.forEach { action ->
            CrashDiagnostics.record(when (action) {
                is Click -> CrashDiagnostics.Event.CLICK
                is Swipe -> CrashDiagnostics.Event.SWIPE
                is Pause -> CrashDiagnostics.Event.PAUSE
                is Intent -> CrashDiagnostics.Event.INTENT
                is ToggleEvent -> CrashDiagnostics.Event.TOGGLE_EVENT
                is ChangeCounter -> CrashDiagnostics.Event.CHANGE_COUNTER
                is ExternalAction -> CrashDiagnostics.Event.EXTERNAL_ACTION
                is Notification -> CrashDiagnostics.Event.NOTIFICATION
                is SystemAction -> CrashDiagnostics.Event.SYSTEM_ACTION
                is SetText -> CrashDiagnostics.Event.SET_TEXT
                is PlaySound -> CrashDiagnostics.Event.PLAY_SOUND
                is CaptureScreenshot -> CrashDiagnostics.Event.CAPTURE_SCREENSHOT
            })
            try {
                when (action) {
                    is Click -> executeClick(event, action, results)
                    is Swipe -> executeSwipe(action)
                    is Pause -> executePause(action)
                    is Intent -> executeIntent(action)
                    is ToggleEvent -> executeToggleEvent(action)
                    is ChangeCounter -> executeChangeCounter(action)
                    is ExternalAction -> executeExternalAction(action)
                    is Notification -> executeNotification(event, action)
                    is SystemAction -> executeSystemAction(action)
                    is SetText -> executeSetText(action)
                    is PlaySound -> executePlaySound(action)
                    is CaptureScreenshot -> executeCaptureScreenshot(action)
                }
            } catch (error: Exception) {
                if (error !is kotlinx.coroutines.CancellationException) CrashDiagnostics.recordFailure(error)
                throw error
            }
        }
    }

    private suspend fun executeClick(event: Event, click: Click, results: ConditionsResults?) {
        val clickPath = when (click.positionType) {
            Click.PositionType.USER_SELECTED -> {
                click.position?.let { position ->
                    Path().apply { moveTo(position, random) }
                }
            }

            Click.PositionType.ON_DETECTED_CONDITION ->
                getOnConditionClickPath(event, click, results)
        } ?: return

        val clickGesture = GestureDescription.Builder().buildSingleStroke(
            path = clickPath,
            durationMs = click.pressDuration!!,
            random = random,
        )

        withContext(Dispatchers.Main) {
            androidExecutor.dispatchGesture(clickGesture)
        }
    }

    private fun getOnConditionClickPath(event: Event, click: Click, results: ConditionsResults?): Path? {
        if (event !is ScreenEvent) return null

        val result = when {
            event.conditionOperator == OR -> results?.getFirstScreenConditionDetectedResult()
            click.clickOnConditionId != null -> results?.getScreenConditionResult(click.clickOnConditionId!!.databaseId)
            else -> null
        }

        if (result == null) {
            Log.w(TAG, "Click is invalid, can't execute")
            return null
        }

        return Path().apply {
            moveTo(
                position = Point(
                    (result.position?.x ?: 0) + (click.clickOffset?.x ?: 0),
                    (result.position?.y ?: 0) + (click.clickOffset?.y ?: 0),
                ),
                random = random,
            )
        }
    }

    /**
     * Execute the provided swipe.
     * @param swipe the swipe to be executed.
     */
    private suspend fun executeSwipe(swipe: Swipe) {
        val swipeGesture = GestureDescription.Builder().buildSingleStroke(
            path =
                if (swipe.from == null || swipe.to == null) return
                else Path().apply { line(swipe.from, swipe.to, random) },
            durationMs = swipe.swipeDuration!!,
            random = random,
        )

        withContext(Dispatchers.Main) {
            androidExecutor.dispatchGesture(swipeGesture)
        }
    }

    /**
     * Execute the provided pause.
     * @param pause the pause to be executed.
     */
    private suspend fun executePause(pause: Pause) {
        delay(pause.pauseDuration!!.getPauseDurationMs(random))
    }

    /**
     * Execute the provided intent.
     * @param intent the intent to be executed.
     */
    private suspend fun executeIntent(intent: Intent) {
        val androidIntent = AndroidIntent().apply {
            action = intent.intentAction!!
            flags = intent.flags!!

            intent.componentName?.let {
                component = intent.componentName
            }

            intent.extras?.forEach { putDomainExtra(it) }
        }

        if (intent.isBroadcast) {
            withContext(Dispatchers.Main) {
                androidExecutor.sendBroadcast(androidIntent)
            }
            delay(INTENT_BROADCAST_DELAY)
        } else {
            withContext(Dispatchers.Main) {
                androidExecutor.startActivity(androidIntent)
            }
            delay(INTENT_START_ACTIVITY_DELAY)
        }
    }

    /**
     * Execute the provided toggle event.
     * @param toggleEvent the toggleEvent to be executed.
     */
    private fun executeToggleEvent(toggleEvent: ToggleEvent) {
        if (toggleEvent.toggleAll) {
            when (toggleEvent.toggleAllType) {
                ToggleEvent.ToggleType.ENABLE -> processingState.enableAll()
                ToggleEvent.ToggleType.DISABLE -> processingState.disableAll()
                ToggleEvent.ToggleType.TOGGLE -> processingState.toggleAll()
                null -> Unit
            }

            return
        }

        toggleEvent.eventToggles.forEach { eventToggle ->
            when (eventToggle.toggleType) {
                ToggleEvent.ToggleType.ENABLE -> processingState.enableEvent(eventToggle.targetEventId!!.databaseId)
                ToggleEvent.ToggleType.DISABLE -> processingState.disableEvent(eventToggle.targetEventId!!.databaseId)
                ToggleEvent.ToggleType.TOGGLE -> processingState.toggleEvent(eventToggle.targetEventId!!.databaseId)
            }
        }
    }

    /**
     * Execute the provided change counter.
     * @param changeCounter the changeCounter action to be executed.
     */
    private fun executeChangeCounter(changeCounter: ChangeCounter) {
        val oldValue = processingState.getCounterValue(changeCounter.counterName) ?: return

        val operandValue = when (val operationValue = changeCounter.operationValue) {
            is CounterOperationValue.Counter -> processingState.getCounterValue(operationValue.value) ?: 0.0
            is CounterOperationValue.Number -> operationValue.value
        }

        processingState.setCounterValue(
            counterName = changeCounter.counterName,
            value = when (changeCounter.operation) {
                ChangeCounter.OperationType.ADD -> oldValue + operandValue
                ChangeCounter.OperationType.MINUS -> oldValue - operandValue
                ChangeCounter.OperationType.SET -> operandValue
            }
        )
    }

    private fun executeExternalAction(externalAction: ExternalAction) {
        androidExecutor.fireExternalAction(externalAction.externalActionName)
    }

    private fun executeNotification(event: Event, notification: Notification) {
        val counters = buildMap {
            notification.messageText.findCounterReferences().forEach { counterName ->
                processingState.getCounterValue(counterName)?.let { counterValue ->
                    put(counterName, counterValue)
                }
            }
        }

        androidExecutor.postNotification(
            ActionNotificationRequest(
                actionId = notification.id.databaseId,
                title = notification.name ?: "Macrion",
                message = notification.messageText.replaceCounterReferences(counters),
                eventId = event.id.databaseId,
                groupName = event.name,
                importance = notification.channelImportance,
            )
        )
    }

    private suspend fun executeSystemAction(action: SystemAction) {
        val globalAction = when (action.type) {
            SystemAction.Type.BACK -> AccessibilityService.GLOBAL_ACTION_BACK
            SystemAction.Type.HOME -> AccessibilityService.GLOBAL_ACTION_HOME
            SystemAction.Type.RECENT_APPS -> AccessibilityService.GLOBAL_ACTION_RECENTS
        }

        withContext(Dispatchers.Main) {
            androidExecutor.performGlobalAction(globalAction)
        }
    }

    private suspend fun executeSetText(action: SetText) {
        val counters = buildMap {
            action.text.findCounterReferences().forEach { counterName ->
                processingState.getCounterValue(counterName)?.let { counterValue ->
                    put(counterName, counterValue)
                }
            }
        }

        withContext(Dispatchers.Main) {
            androidExecutor.writeTextOnFocusedItem(
                text = action.text.replaceCounterReferences(counters),
                validate = action.validateInput,
            )
        }
    }

    private fun executePlaySound(action: PlaySound) {
        val soundUri = action.soundUri ?: return
        androidExecutor.playSound(soundUri)
    }

    private suspend fun executeCaptureScreenshot(action: CaptureScreenshot) {
        androidExecutor.captureScreenshot(action.screenshotFolderUri, action.screenshotFolderName)
    }
}

/** Tag for logs. */
private const val TAG = "ActionExecutor"
/** Waiting delay after a start activity to avoid overflowing the system. */
private const val INTENT_START_ACTIVITY_DELAY = 1000L
/** Waiting delay after a broadcast to avoid overflowing the system. */
private const val INTENT_BROADCAST_DELAY = 100L
