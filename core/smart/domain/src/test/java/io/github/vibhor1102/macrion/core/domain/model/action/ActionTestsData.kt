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
package io.github.vibhor1102.macrion.core.domain.model.action

import android.content.ComponentName
import android.content.Intent
import android.graphics.Point
import io.github.vibhor1102.macrion.core.base.identifier.Identifier
import io.github.vibhor1102.macrion.core.database.entity.ActionEntity
import io.github.vibhor1102.macrion.core.database.entity.ActionType
import io.github.vibhor1102.macrion.core.database.entity.ChangeCounterOperationType
import io.github.vibhor1102.macrion.core.database.entity.ClickPositionType
import io.github.vibhor1102.macrion.core.database.entity.CompleteActionEntity
import io.github.vibhor1102.macrion.core.database.entity.CounterOperationValueType
import io.github.vibhor1102.macrion.core.database.entity.EventToggleEntity
import io.github.vibhor1102.macrion.core.database.entity.IntentExtraEntity
import io.github.vibhor1102.macrion.core.database.entity.IntentExtraType
import io.github.vibhor1102.macrion.core.database.entity.SystemActionType
import io.github.vibhor1102.macrion.core.domain.model.counter.CounterOperationValue
import io.github.vibhor1102.macrion.core.domain.model.action.intent.IntentExtra
import io.github.vibhor1102.macrion.core.domain.model.action.toggleevent.EventToggle
import io.github.vibhor1102.macrion.core.domain.model.event.EventTestsData
import io.github.vibhor1102.macrion.core.domain.utils.asIdentifier

internal object ActionTestsData {

    const val ACTION_EVENT_ID = EventTestsData.EVENT_ID

    /* ------- Click Action Data ------- */

    private const val CLICK_ID = 7L
    private const val CLICK_NAME = "Click name"
    private const val CLICK_PRESS_DURATION = 250L
    private const val CLICK_X_POSITION = 24
    private const val CLICK_Y_POSITION = 87

    fun getNewClickEntity(
        id: Long = CLICK_ID,
        name: String = CLICK_NAME,
        priority: Int = 0,
        pressDuration: Long = CLICK_PRESS_DURATION,
        x: Int? = CLICK_X_POSITION,
        y: Int? = CLICK_Y_POSITION,
        clickOnConditionId: Long? = null,
        positionType: ClickPositionType =
            if (x != null && y != null) ClickPositionType.USER_SELECTED
            else ClickPositionType.ON_DETECTED_CONDITION,
        eventId: Long,
    ) = CompleteActionEntity(
        action = ActionEntity(id, eventId, priority, name, ActionType.CLICK, x = x, y = y,
            clickOnConditionId = clickOnConditionId, clickPositionType = positionType,
            pressDuration = pressDuration),
        intentExtras = emptyList(),
        eventsToggle = emptyList(),
    )

    fun getNewClick(
        id: Long = CLICK_ID,
        name: String? = CLICK_NAME,
        priority: Int = 0,
        pressDuration: Long? = CLICK_PRESS_DURATION,
        x: Int? = CLICK_X_POSITION,
        y: Int? = CLICK_Y_POSITION,
        clickOnConditionId: Long? = null,
        positionType: Click.PositionType =
            if (x != null && y != null) Click.PositionType.USER_SELECTED
            else Click.PositionType.ON_DETECTED_CONDITION,
        eventId: Long,
    ) = Click(id.asIdentifier(), eventId.asIdentifier(), name, priority, pressDuration, positionType,
        if (x != null && y != null) Point(x, y) else null,
        clickOnConditionId?.let { Identifier(databaseId = clickOnConditionId) },
    )


    /* ------- Swipe Action Data ------- */

    private const val SWIPE_ID = 8L
    private const val SWIPE_NAME = "Swipe name"
    private const val SWIPE_DURATION = 1000L
    private const val SWIPE_FROM_X_POSITION = 42
    private const val SWIPE_FROM_Y_POSITION = 78
    private const val SWIPE_TO_X_POSITION = 789
    private const val SWIPE_TO_Y_POSITION = 1445

    fun getNewSwipeEntity(
        id: Long = SWIPE_ID,
        name: String = SWIPE_NAME,
        priority: Int = 0,
        swipeDuration: Long = SWIPE_DURATION,
        fromX: Int = SWIPE_FROM_X_POSITION,
        fromY: Int = SWIPE_FROM_Y_POSITION,
        toX: Int = SWIPE_TO_X_POSITION,
        toY: Int = SWIPE_TO_Y_POSITION,
        eventId: Long,
    ) = CompleteActionEntity(
        action = ActionEntity(id, eventId, priority, name, ActionType.SWIPE, fromX = fromX, fromY = fromY, toX = toX,
            toY = toY, swipeDuration = swipeDuration),
        intentExtras = emptyList(),
        eventsToggle = emptyList(),
    )

    fun getNewSwipe(
        id: Long = SWIPE_ID,
        name: String? = SWIPE_NAME,
        priority: Int = 0,
        swipeDuration: Long? = SWIPE_DURATION,
        fromX: Int? = SWIPE_FROM_X_POSITION,
        fromY: Int? = SWIPE_FROM_Y_POSITION,
        toX: Int? = SWIPE_TO_X_POSITION,
        toY: Int? = SWIPE_TO_Y_POSITION,
        eventId: Long,
    ) : Swipe = Swipe(id.asIdentifier(), eventId.asIdentifier(), name, priority, swipeDuration,
        if (fromX != null && fromY != null) Point(fromX, fromY) else null,
        if (toX != null && toY != null) Point(toX, toY) else null,
    )


    /* ------- Pause Action Data ------- */

    private const val PAUSE_ID = 9L
    private const val PAUSE_NAME = "Pause name"
    private const val PAUSE_DURATION = 500L

    fun getNewPauseEntity(
        id: Long = PAUSE_ID,
        name: String = PAUSE_NAME,
        priority: Int = 0,
        pauseDuration: Long = PAUSE_DURATION,
        eventId: Long,
    ) = CompleteActionEntity(
        action = ActionEntity(id, eventId, priority, name, ActionType.PAUSE, pauseDuration = pauseDuration),
        intentExtras = emptyList(),
        eventsToggle = emptyList(),
    )

    fun getNewPause(
        id: Long = PAUSE_ID,
        name: String? = PAUSE_NAME,
        priority: Int = 0,
        pauseDuration: Long? = PAUSE_DURATION,
        eventId: Long,
    ) = Pause(id.asIdentifier(), eventId.asIdentifier(), name, priority, pauseDuration)


    /* ------- Intent Action Data ------- */

    private const val INTENT_ID = 149L
    private const val INTENT_NAME = "Intent name"
    private const val INTENT_IS_ADVANCED = true
    private const val INTENT_IS_BROADCAST = true
    private const val INTENT_ACTION = "com.toto.tata.ACTION_TOTO"
    private const val INTENT_COMPONENT_NAME_STRING = "com.toto.tata/com.toto.tata.Activity"
    private val INTENT_COMPONENT_NAME = ComponentName.unflattenFromString("com.toto.tata/com.toto.tata.Activity")
    private const val INTENT_FLAGS = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK

    fun getNewIntentEntity(
        id: Long = INTENT_ID,
        name: String = INTENT_NAME,
        priority: Int = 0,
        isAdvanced: Boolean = INTENT_IS_ADVANCED,
        isBroadcast: Boolean = INTENT_IS_BROADCAST,
        action: String = INTENT_ACTION,
        componentName: String = INTENT_COMPONENT_NAME_STRING,
        flags: Int = INTENT_FLAGS,
        eventId: Long,
        intentExtras: List<IntentExtraEntity> = emptyList()
    ) = CompleteActionEntity(
        action = ActionEntity(id, eventId, priority, name, ActionType.INTENT, isAdvanced = isAdvanced,
            isBroadcast = isBroadcast, intentAction = action, componentName = componentName, flags = flags),
        intentExtras = intentExtras,
        eventsToggle = emptyList(),
    )

    fun getNewIntent(
        id: Long = INTENT_ID,
        name: String? = INTENT_NAME,
        priority: Int = 0,
        isAdvanced: Boolean = INTENT_IS_ADVANCED,
        isBroadcast: Boolean = INTENT_IS_BROADCAST,
        action: String = INTENT_ACTION,
        componentName: ComponentName = INTENT_COMPONENT_NAME!!,
        flags: Int = INTENT_FLAGS,
        eventId: Long,
        intentExtras: MutableList<IntentExtra<out Any>> = mutableListOf()
    ) = Intent(id.asIdentifier(), eventId.asIdentifier(), name, priority, isAdvanced, isBroadcast, action, componentName, flags, intentExtras)


    /* ------- Intent Extra Data ------- */

    private const val INTENT_EXTRA_ID = 547L
    private const val INTENT_EXTRA_ACTION_ID = INTENT_ID
    private val INTENT_EXTRA_TYPE = IntentExtraType.INTEGER
    private const val INTENT_EXTRA_KEY = "toto"

    fun getNewIntentExtraEntity(
        id: Long = INTENT_EXTRA_ID,
        actionId: Long = INTENT_EXTRA_ACTION_ID,
        type: IntentExtraType = INTENT_EXTRA_TYPE,
        key: String = INTENT_EXTRA_KEY,
        value: String,
    ) = IntentExtraEntity(id, actionId, type, key, value)

    fun <T> getNewIntentExtra(
        id: Long = INTENT_EXTRA_ID,
        actionId: Long = INTENT_EXTRA_ACTION_ID,
        key: String = INTENT_EXTRA_KEY,
        value: T,
    ) = IntentExtra(id.asIdentifier(), actionId.asIdentifier(), key, value)


    /* ------- Toggle Event Action Data ------- */

    private const val TOGGLE_EVENT_ID = 159L
    private const val TOGGLE_EVENT_NAME = "Toggle name"
    private const val TOGGLE_EVENT_TOGGLE_ALL = true
    private val TOGGLE_EVENT_TOGGLE_ALL_TYPE = ToggleEvent.ToggleType.TOGGLE

    fun getNewToggleEventEntity(
        id: Long = TOGGLE_EVENT_ID,
        name: String = TOGGLE_EVENT_NAME,
        priority: Int = 0,
        toggleAll: Boolean = TOGGLE_EVENT_TOGGLE_ALL,
        toggleType: ToggleEvent.ToggleType = TOGGLE_EVENT_TOGGLE_ALL_TYPE,
        eventToggles: List<EventToggleEntity> = emptyList(),
        eventId: Long,
    ) = CompleteActionEntity(
        action = ActionEntity(id, eventId, priority, name, ActionType.TOGGLE_EVENT, toggleAll = toggleAll, toggleAllType = toggleType.toEntity()),
        intentExtras = emptyList(),
        eventsToggle = eventToggles,
    )

    fun getNewToggleEvent(
        id: Long = TOGGLE_EVENT_ID,
        name: String? = TOGGLE_EVENT_NAME,
        priority: Int = 0,
        toggleAll: Boolean = TOGGLE_EVENT_TOGGLE_ALL,
        toggleType: ToggleEvent.ToggleType = TOGGLE_EVENT_TOGGLE_ALL_TYPE,
        eventToggle: MutableList<EventToggle> = mutableListOf(),
        eventId: Long,
    ) = ToggleEvent(id.asIdentifier(), eventId.asIdentifier(), name, priority, toggleAll, toggleType, eventToggle)


    /* ------- Event toggle Data ------- */

    private const val EVENT_TOGGLE_ID = 875L
    private const val EVENT_TOGGLE_ACTION_ID = TOGGLE_EVENT_ID
    private const val EVENT_TOGGLE_TARGET_ID = 562L
    private val EVENT_TOGGLE_TYPE = ToggleEvent.ToggleType.TOGGLE

    fun getNewEventToggleEntity(
        id: Long = EVENT_TOGGLE_ID,
        actionId: Long = EVENT_TOGGLE_ACTION_ID,
        targetEventId: Long = EVENT_TOGGLE_TARGET_ID,
        type: ToggleEvent.ToggleType = EVENT_TOGGLE_TYPE,
    ) = EventToggleEntity(id, actionId, type.toEntity(), targetEventId)

    /* ------- Change Counter Action Data ------- */

    private const val CHANGE_COUNTER_ID = 214L
    private const val CHANGE_COUNTER_NAME = "ChangeCounter name"
    private const val CHANGE_COUNTER_COUNTER_NAME = "myCounter"
    private val CHANGE_COUNTER_OPERATION = ChangeCounter.OperationType.ADD
    private const val CHANGE_COUNTER_VALUE = 5.0

    fun getNewChangeCounterEntity(
        id: Long = CHANGE_COUNTER_ID,
        name: String = CHANGE_COUNTER_NAME,
        priority: Int = 0,
        counterName: String = CHANGE_COUNTER_COUNTER_NAME,
        operation: ChangeCounter.OperationType = CHANGE_COUNTER_OPERATION,
        operationValue: Double = CHANGE_COUNTER_VALUE,
        eventId: Long,
    ) = CompleteActionEntity(
        action = ActionEntity(
            id, eventId, priority, name, ActionType.CHANGE_COUNTER,
            counterName = counterName,
            counterOperation = ChangeCounterOperationType.valueOf(operation.name),
            counterOperationValueType = CounterOperationValueType.NUMBER,
            counterOperationValue = operationValue,
        ),
        intentExtras = emptyList(),
        eventsToggle = emptyList(),
    )

    fun getNewChangeCounter(
        id: Long = CHANGE_COUNTER_ID,
        name: String? = CHANGE_COUNTER_NAME,
        priority: Int = 0,
        counterName: String = CHANGE_COUNTER_COUNTER_NAME,
        operation: ChangeCounter.OperationType = CHANGE_COUNTER_OPERATION,
        operationValue: CounterOperationValue = CounterOperationValue.Number(CHANGE_COUNTER_VALUE),
        eventId: Long,
    ) = ChangeCounter(id.asIdentifier(), eventId.asIdentifier(), name, priority, counterName, operation, operationValue)


    /* ------- Notification Action Data ------- */

    private const val NOTIFICATION_ID = 312L
    private const val NOTIFICATION_NAME = "Notification name"
    private const val NOTIFICATION_MESSAGE = "Hello from Macrion"
    private const val NOTIFICATION_IMPORTANCE = 3

    fun getNewNotificationEntity(
        id: Long = NOTIFICATION_ID,
        name: String = NOTIFICATION_NAME,
        priority: Int = 0,
        messageText: String = NOTIFICATION_MESSAGE,
        channelImportance: Int = NOTIFICATION_IMPORTANCE,
        eventId: Long,
    ) = CompleteActionEntity(
        action = ActionEntity(
            id, eventId, priority, name, ActionType.NOTIFICATION,
            notificationMessageText = messageText,
            notificationImportance = channelImportance,
        ),
        intentExtras = emptyList(),
        eventsToggle = emptyList(),
    )

    fun getNewNotification(
        id: Long = NOTIFICATION_ID,
        name: String? = NOTIFICATION_NAME,
        priority: Int = 0,
        messageText: String = NOTIFICATION_MESSAGE,
        channelImportance: Int = NOTIFICATION_IMPORTANCE,
        eventId: Long,
    ) = Notification(id.asIdentifier(), eventId.asIdentifier(), name, priority, messageText, channelImportance)


    /* ------- System Action Data ------- */

    private const val SYSTEM_ACTION_ID = 421L
    private const val SYSTEM_ACTION_NAME = "SystemAction name"
    private val SYSTEM_ACTION_TYPE = SystemAction.Type.BACK

    fun getNewSystemActionEntity(
        id: Long = SYSTEM_ACTION_ID,
        name: String = SYSTEM_ACTION_NAME,
        priority: Int = 0,
        type: SystemAction.Type = SYSTEM_ACTION_TYPE,
        eventId: Long,
    ) = CompleteActionEntity(
        action = ActionEntity(
            id, eventId, priority, name, ActionType.SYSTEM,
            systemActionType = SystemActionType.valueOf(type.name),
        ),
        intentExtras = emptyList(),
        eventsToggle = emptyList(),
    )

    fun getNewSystemAction(
        id: Long = SYSTEM_ACTION_ID,
        name: String? = SYSTEM_ACTION_NAME,
        priority: Int = 0,
        type: SystemAction.Type = SYSTEM_ACTION_TYPE,
        eventId: Long,
    ) = SystemAction(id.asIdentifier(), eventId.asIdentifier(), name, priority, type)


    /* ------- Set Text Action Data ------- */

    private const val SET_TEXT_ID = 534L
    private const val SET_TEXT_NAME = "SetText name"
    private const val SET_TEXT_VALUE = "Hello World"
    private const val SET_TEXT_VALIDATE_INPUT = true

    fun getNewSetTextEntity(
        id: Long = SET_TEXT_ID,
        name: String = SET_TEXT_NAME,
        priority: Int = 0,
        text: String = SET_TEXT_VALUE,
        validateInput: Boolean = SET_TEXT_VALIDATE_INPUT,
        eventId: Long,
    ) = CompleteActionEntity(
        action = ActionEntity(
            id, eventId, priority, name, ActionType.TEXT,
            textValue = text,
            textValidateInput = validateInput,
        ),
        intentExtras = emptyList(),
        eventsToggle = emptyList(),
    )

    fun getNewSetText(
        id: Long = SET_TEXT_ID,
        name: String? = SET_TEXT_NAME,
        priority: Int = 0,
        text: String = SET_TEXT_VALUE,
        validateInput: Boolean = SET_TEXT_VALIDATE_INPUT,
        eventId: Long,
    ) = SetText(id.asIdentifier(), eventId.asIdentifier(), name, priority, text, validateInput)

    /* ------- External Action Data ------- */

    private const val EXTERNAL_ACTION_ID = 51L
    private const val EXTERNAL_ACTION_NAME = "External action name"
    private const val EXTERNAL_ACTION_LINK_NAME = "Open xyz game intent"

    fun getNewExternalActionEntity(
        id: Long = EXTERNAL_ACTION_ID,
        name: String = EXTERNAL_ACTION_NAME,
        priority: Int = 0,
        externalActionName: String = EXTERNAL_ACTION_LINK_NAME,
        eventId: Long,
    ) = CompleteActionEntity(
        action = ActionEntity(
            id, eventId, priority, name, ActionType.EXTERNAL_ACTION,
            externalActionName = externalActionName,
        ),
        intentExtras = emptyList(),
        eventsToggle = emptyList(),
    )

    fun getNewExternalAction(
        id: Long = EXTERNAL_ACTION_ID,
        name: String? = EXTERNAL_ACTION_NAME,
        priority: Int = 0,
        externalActionName: String = EXTERNAL_ACTION_LINK_NAME,
        eventId: Long,
    ) = ExternalAction(id.asIdentifier(), eventId.asIdentifier(), name, priority, externalActionName)

    /* ------- Play Sound Data ------- */

    private const val PLAY_SOUND_ID = 61L
    private const val PLAY_SOUND_NAME = "Play sound name"
    private const val PLAY_SOUND_URI = "content://settings/system/notification_sound"
    private const val PLAY_SOUND_TITLE = "Chime"

    fun getNewPlaySoundEntity(
        id: Long = PLAY_SOUND_ID,
        name: String = PLAY_SOUND_NAME,
        priority: Int = 0,
        soundUri: String? = PLAY_SOUND_URI,
        soundTitle: String? = PLAY_SOUND_TITLE,
        eventId: Long,
    ) = CompleteActionEntity(
        action = ActionEntity(
            id, eventId, priority, name, ActionType.PLAY_SOUND,
            soundUri = soundUri,
            soundTitle = soundTitle,
        ),
        intentExtras = emptyList(),
        eventsToggle = emptyList(),
    )

    fun getNewPlaySound(
        id: Long = PLAY_SOUND_ID,
        name: String? = PLAY_SOUND_NAME,
        priority: Int = 0,
        soundUri: String? = PLAY_SOUND_URI,
        soundTitle: String? = PLAY_SOUND_TITLE,
        eventId: Long,
    ) = PlaySound(id.asIdentifier(), eventId.asIdentifier(), name, priority, soundUri, soundTitle)

    /* ------- Capture Screenshot Action Data ------- */

    private const val CAPTURE_SCREENSHOT_ID = 23L
    private const val CAPTURE_SCREENSHOT_NAME = "Capture screenshot name"
    private const val CAPTURE_SCREENSHOT_FOLDER_URI = "content://com.android.externalstorage.documents/tree/primary%3APictures"
    private const val CAPTURE_SCREENSHOT_FOLDER_NAME = "Pictures"

    fun getNewCaptureScreenshotEntity(
        id: Long = CAPTURE_SCREENSHOT_ID,
        name: String = CAPTURE_SCREENSHOT_NAME,
        priority: Int = 0,
        screenshotFolderUri: String? = CAPTURE_SCREENSHOT_FOLDER_URI,
        screenshotFolderName: String? = CAPTURE_SCREENSHOT_FOLDER_NAME,
        eventId: Long,
    ) = CompleteActionEntity(
        action = ActionEntity(
            id, eventId, priority, name, ActionType.CAPTURE_SCREENSHOT,
            screenshotFolderUri = screenshotFolderUri,
            screenshotFolderName = screenshotFolderName,
        ),
        intentExtras = emptyList(),
        eventsToggle = emptyList(),
    )

    fun getNewCaptureScreenshot(
        id: Long = CAPTURE_SCREENSHOT_ID,
        name: String? = CAPTURE_SCREENSHOT_NAME,
        priority: Int = 0,
        screenshotFolderUri: String? = CAPTURE_SCREENSHOT_FOLDER_URI,
        screenshotFolderName: String? = CAPTURE_SCREENSHOT_FOLDER_NAME,
        eventId: Long,
    ) = CaptureScreenshot(id.asIdentifier(), eventId.asIdentifier(), name, priority, screenshotFolderUri, screenshotFolderName)

    /* ------- Split Action Data ------- */

    private const val SPLIT_ACTION_ID = 88L
    private const val SPLIT_ACTION_NAME = "Zoom"

    fun getNewSplitActionEntity(
        id: Long = SPLIT_ACTION_ID,
        name: String = SPLIT_ACTION_NAME,
        priority: Int = 0,
        eventId: Long,
        splitItems: List<io.github.vibhor1102.macrion.core.database.entity.SplitActionItemEntity> = listOf(
            io.github.vibhor1102.macrion.core.database.entity.SplitActionItemEntity(
                id = 1L,
                actionId = id,
                priority = 0,
                type = ActionType.SWIPE,
                fromX = 100,
                fromY = 200,
                toX = 300,
                toY = 400,
                duration = 350L,
            ),
            io.github.vibhor1102.macrion.core.database.entity.SplitActionItemEntity(
                id = 2L,
                actionId = id,
                priority = 1,
                type = ActionType.SWIPE,
                fromX = 500,
                fromY = 600,
                toX = 700,
                toY = 800,
                duration = 350L,
            ),
        ),
    ) = CompleteActionEntity(
        action = ActionEntity(
            id, eventId, priority, name, ActionType.SPLIT_ACTION,
        ),
        splitItems = splitItems,
        intentExtras = emptyList(),
        eventsToggle = emptyList(),
    )

    fun getNewSplitAction(
        id: Long = SPLIT_ACTION_ID,
        name: String? = SPLIT_ACTION_NAME,
        priority: Int = 0,
        eventId: Long,
        subActions: List<Action> = listOf(
            Swipe(
                id = 1L.asIdentifier(),
                eventId = eventId.asIdentifier(),
                name = "Swipe 1",
                priority = 0,
                from = Point(100, 200),
                to = Point(300, 400),
                swipeDuration = 350L,
            ),
            Swipe(
                id = 2L.asIdentifier(),
                eventId = eventId.asIdentifier(),
                name = "Swipe 2",
                priority = 1,
                from = Point(500, 600),
                to = Point(700, 800),
                swipeDuration = 350L,
            ),
        ),
    ) = SplitAction(id.asIdentifier(), eventId.asIdentifier(), name, priority, subActions)

    fun getNewEventToggleExtra(
        id: Long = EVENT_TOGGLE_ID,
        actionId: Long = EVENT_TOGGLE_ACTION_ID,
        targetEventId: Long = EVENT_TOGGLE_TARGET_ID,
        type: ToggleEvent.ToggleType = EVENT_TOGGLE_TYPE,
    ) = EventToggle(id.asIdentifier(), actionId.asIdentifier(), targetEventId.asIdentifier(), type)
}
