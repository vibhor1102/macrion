/*
 * Copyright (C) 2024 Kevin Buzeau
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
package io.github.vibhor1102.macrion.feature.tutorial.ui.dialogs

import android.content.Context
import androidx.activity.ComponentDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.vibhor1102.macrion.core.common.tutorial.domain.TutorialRepository
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.Tip
import io.github.vibhor1102.macrion.core.ui.compose.MacrionDialogSurface
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.core.ui.utils.getDynamicColorsContext
import io.github.vibhor1102.macrion.feature.tutorial.R
import io.github.vibhor1102.macrion.core.ui.R as UiR

@Composable
fun StopWithVolumeDownTipDialog(
    tutorialRepository: TutorialRepository,
    onDismiss: () -> Unit,
) {
    var dontShowAgain by remember { mutableStateOf(false) }
    Dialog(
        onDismissRequest = {
            if (dontShowAgain) tutorialRepository.dontShowTipAgain(Tip.STOP_WITH_VOLUME_DOWN)
            onDismiss()
        },
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            MacrionDialogSurface {
                StopWithVolumeDownTipContent(
                    dontShowAgain = dontShowAgain,
                    onDontShowAgainChanged = { dontShowAgain = it },
                    onDismiss = {
                        if (dontShowAgain) tutorialRepository.dontShowTipAgain(Tip.STOP_WITH_VOLUME_DOWN)
                        onDismiss()
                    },
                )
            }
        }
    }
}


internal fun Context.createStopWithVolumeDownTutorialDialog(
    tutorialRepository: TutorialRepository,
    onDismissed: (() -> Unit)?,
): android.app.Dialog {
    val dialogContext = getDynamicColorsContext(R.style.AppTheme)
    var dontShowAgain by mutableStateOf(false)
    lateinit var dialog: ComponentDialog
    val content = ComposeView(dialogContext).apply {
        setContent {
            MacrionTheme {
                MacrionDialogSurface {
                    StopWithVolumeDownTipContent(
                        dontShowAgain = dontShowAgain,
                        onDontShowAgainChanged = { dontShowAgain = it },
                        onDismiss = { dialog.dismiss() },
                    )
                }
            }
        }
    }

    dialog = ComponentDialog(dialogContext).apply {
        setContentView(content)
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        setOnDismissListener {
            if (dontShowAgain) tutorialRepository.dontShowTipAgain(Tip.STOP_WITH_VOLUME_DOWN)
            onDismissed?.invoke()
        }
    }
    return dialog
}

@Composable
private fun StopWithVolumeDownTipContent(
    dontShowAgain: Boolean,
    onDontShowAgainChanged: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.dialog_title_tutorial),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        HorizontalDivider()
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_volume_down),
                contentDescription = null,
                modifier = Modifier.padding(top = 16.dp).size(50.dp),
            )
            Text(
                text = stringResource(R.string.message_tutorial_volume_down_stop),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Row(
                modifier = Modifier
                    .clickable { onDontShowAgainChanged(!dontShowAgain) }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = dontShowAgain,
                    onCheckedChange = onDontShowAgainChanged,
                )
                Text(stringResource(R.string.message_dont_show_again))
            }
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 8.dp),
            ) {
                Text(stringResource(UiR.string.button_understood))
            }
        }
    }
}
