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
package io.github.vibhor1102.macrion.feature.smart.config.ui.action.brief

import android.content.Context
import io.github.vibhor1102.macrion.core.common.overlays.base.BaseOverlay
import io.github.vibhor1102.macrion.core.common.permissions.model.PermissionPostNotification
import io.github.vibhor1102.macrion.core.domain.model.action.Action
import io.github.vibhor1102.macrion.core.domain.model.action.ChangeCounter
import io.github.vibhor1102.macrion.core.domain.model.action.Click
import io.github.vibhor1102.macrion.core.domain.model.action.Intent
import io.github.vibhor1102.macrion.core.domain.model.action.Notification
import io.github.vibhor1102.macrion.core.domain.model.action.Pause
import io.github.vibhor1102.macrion.core.domain.model.action.PlaySound
import io.github.vibhor1102.macrion.core.domain.model.action.CaptureScreenshot
import io.github.vibhor1102.macrion.core.domain.model.action.SetText
import io.github.vibhor1102.macrion.core.domain.model.action.Swipe
import io.github.vibhor1102.macrion.core.domain.model.action.SystemAction
import io.github.vibhor1102.macrion.core.domain.model.action.ToggleEvent
import io.github.vibhor1102.macrion.feature.smart.config.ui.action.OnActionConfigCompleteListener
import io.github.vibhor1102.macrion.feature.smart.config.ui.action.changecounter.ChangeCounterDialog
import io.github.vibhor1102.macrion.feature.smart.config.ui.action.click.ClickDialog
import io.github.vibhor1102.macrion.feature.smart.config.ui.action.intent.IntentDialog
import io.github.vibhor1102.macrion.feature.smart.config.ui.action.notification.NotificationDialog
import io.github.vibhor1102.macrion.feature.smart.config.ui.action.pause.PauseDialog
import io.github.vibhor1102.macrion.feature.smart.config.ui.action.selection.ActionTypeChoice
import io.github.vibhor1102.macrion.feature.smart.config.ui.action.selection.ActionTypeSelectionDialog
import io.github.vibhor1102.macrion.feature.smart.config.ui.action.settext.SetTextDialog
import io.github.vibhor1102.macrion.feature.smart.config.ui.action.sound.PlaySoundDialog
import io.github.vibhor1102.macrion.feature.smart.config.ui.action.screenshot.CaptureScreenshotDialog
import io.github.vibhor1102.macrion.feature.smart.config.ui.action.swipe.SwipeDialog
import io.github.vibhor1102.macrion.feature.smart.config.ui.action.system.SystemActionDialog
import io.github.vibhor1102.macrion.feature.smart.config.ui.action.toggleevent.ToggleEventDialog
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.starters.newNotificationPermissionStarterOverlay
import io.github.vibhor1102.macrion.feature.smart.config.ui.copy.action.ActionCopyDialog
import io.github.vibhor1102.macrion.core.domain.model.action.ExternalAction
import io.github.vibhor1102.macrion.feature.smart.config.ui.action.external.ExternalActionDialog


import io.github.vibhor1102.macrion.core.domain.model.action.SplitAction

import io.github.vibhor1102.macrion.feature.smart.config.ui.action.split.SplitActionDialog

internal interface ActionConfigurator {
    fun getActionTypeChoices(): List<ActionTypeChoice>
    fun createAction(context: Context, choice: ActionTypeChoice): Action
    fun startActionEdition(action: Action)
    fun startSubActionEdition(parent: SplitAction, subIndex: Int)
    fun upsertEditedAction()
    fun removeEditedAction()
    fun dismissEditedAction()
}

internal fun BaseOverlay.showActionTypeSelectionDialog(configurator: ActionConfigurator) {
    overlayManager.navigateTo(
        context = context,
        newOverlay = ActionTypeSelectionDialog(
            choices = configurator.getActionTypeChoices(),
            onChoiceSelectedListener = { choiceClicked ->
                if (choiceClicked is ActionTypeChoice.Copy) {
                    showActionCopyDialog(configurator)
                    return@ActionTypeSelectionDialog
                }

                val action = configurator.createAction(context, choiceClicked)
                showActionConfigDialog(configurator, action)
            },
        ),
    )
}

internal fun BaseOverlay.showActionCopyDialog(configurator: ActionConfigurator) {
    overlayManager.navigateTo(
        context = context,
        newOverlay = ActionCopyDialog(
            onActionsCopied = { newCopyActions ->
                if (newCopyActions.size == 1)
                    showActionConfigDialog(configurator, newCopyActions[0])
            }
        ),
    )
}

internal fun BaseOverlay.showSubActionConfigDialog(
    configurator: ActionConfigurator,
    parent: SplitAction,
    subIndex: Int,
) {
    val subAction = parent.subActions.getOrNull(subIndex) ?: return
    configurator.startSubActionEdition(parent, subIndex)

    val actionConfigDialogListener = object : OnActionConfigCompleteListener {
        override fun onConfirmClicked() { configurator.upsertEditedAction() }
        override fun onDeleteClicked() { configurator.removeEditedAction() }
        override fun onDismissClicked() { configurator.dismissEditedAction() }
    }

    val overlay = when (subAction) {
        is Click -> ClickDialog(actionConfigDialogListener, canDelete = parent.subActions.size > 2)
        is Swipe -> SwipeDialog(actionConfigDialogListener, canDelete = parent.subActions.size > 2)
        is Pause -> PauseDialog(actionConfigDialogListener)
        is Intent -> IntentDialog(actionConfigDialogListener)
        is SystemAction -> SystemActionDialog(actionConfigDialogListener)
        is ToggleEvent -> ToggleEventDialog(actionConfigDialogListener)
        is ChangeCounter -> ChangeCounterDialog(actionConfigDialogListener)
        is ExternalAction -> ExternalActionDialog(actionConfigDialogListener)
        is SetText -> SetTextDialog(actionConfigDialogListener)
        is PlaySound -> PlaySoundDialog(actionConfigDialogListener)
        is CaptureScreenshot -> CaptureScreenshotDialog(actionConfigDialogListener)
        is Notification -> {
            if (PermissionPostNotification().checkIfGranted(context)) NotificationDialog(actionConfigDialogListener)
            else newNotificationPermissionStarterOverlay(context)
        }
        is SplitAction -> return
    }

    overlayManager.navigateTo(
        context = context,
        newOverlay = overlay,
        hideCurrent = true,
    )
}

internal fun BaseOverlay.showActionConfigDialog(configurator: ActionConfigurator, action: Action) {
    configurator.startActionEdition(action)

    val actionConfigDialogListener: OnActionConfigCompleteListener by lazy {
        object : OnActionConfigCompleteListener {
            override fun onConfirmClicked() { configurator.upsertEditedAction() }
            override fun onDeleteClicked() { configurator.removeEditedAction() }
            override fun onDismissClicked() { configurator.dismissEditedAction() }
        }
    }

    val overlay = when (action) {
        is SplitAction -> SplitActionDialog(
            listener = actionConfigDialogListener,
            onConfigureSubAction = { parent, subIndex ->
                showSubActionConfigDialog(configurator, parent, subIndex)
            },
        )
        is Click -> ClickDialog(actionConfigDialogListener)
        is Swipe -> SwipeDialog(actionConfigDialogListener)
        is Pause -> PauseDialog(actionConfigDialogListener)
        is Intent -> IntentDialog(actionConfigDialogListener)
        is SystemAction -> SystemActionDialog(actionConfigDialogListener)
        is ToggleEvent -> ToggleEventDialog(actionConfigDialogListener)
        is ChangeCounter -> ChangeCounterDialog(actionConfigDialogListener)
        is ExternalAction -> ExternalActionDialog(actionConfigDialogListener)
        is SetText -> SetTextDialog(actionConfigDialogListener)
        is PlaySound -> PlaySoundDialog(actionConfigDialogListener)
        is CaptureScreenshot -> CaptureScreenshotDialog(actionConfigDialogListener)
        is Notification -> {
            if (PermissionPostNotification().checkIfGranted(context)) NotificationDialog(actionConfigDialogListener)
            else newNotificationPermissionStarterOverlay(context)
        }
    }

    overlayManager.navigateTo(
        context = context,
        newOverlay = overlay,
        hideCurrent = true,
    )
}
