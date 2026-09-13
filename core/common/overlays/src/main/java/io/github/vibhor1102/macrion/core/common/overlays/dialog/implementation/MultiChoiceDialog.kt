/* Copyright (C) 2024 Kevin Buzeau; Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.core.common.overlays.dialog.implementation

import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.vibhor1102.macrion.core.common.overlays.R
import io.github.vibhor1102.macrion.core.common.overlays.dialog.OverlayDialog
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme

/** A Compose-native overlay dialog displaying a list of choices. */
open class MultiChoiceDialog<T : DialogChoice>(
    @StyleRes theme: Int,
    @field:StringRes private val dialogTitleText: Int,
    private val choices: List<T>,
    private val onChoiceSelected: (T) -> Unit,
    private val onCanceled: (() -> Unit)? = null,
) : OverlayDialog(theme) {
    override fun onCreateView(): ViewGroup = ComposeView(context).apply {
        setContent {
            MacrionTheme {
                MultiChoiceDialogContent(
                    title = dialogTitleText,
                    choices = choices,
                    onDismiss = { debounceUserInteraction { onCanceled?.invoke(); back() } },
                    onChoiceSelected = { choice ->
                        debounceUserInteraction { back(); onChoiceSelected(choice) }
                    },
                )
            }
        }
    }

}

@Composable
private fun <T : DialogChoice> MultiChoiceDialogContent(
    @StringRes title: Int,
    choices: List<T>,
    onDismiss: () -> Unit,
    onChoiceSelected: (T) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(bottom = 16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onDismiss) {
                Icon(
                    painterResource(R.drawable.ic_cancel),
                    stringResource(android.R.string.cancel),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                stringResource(title),
                Modifier.weight(1f).padding(horizontal = 8.dp),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
        HorizontalDivider()
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 520.dp)) {
            items(choices) { choice -> ChoiceRow(choice) { onChoiceSelected(choice) } }
        }
    }
}

@Composable
private fun ChoiceRow(choice: DialogChoice, onClick: () -> Unit) {
    val detailed = choice.description != null || choice.iconId != null
    val alpha = if (choice.enabled) ENABLED_ITEM_ALPHA else DISABLED_ITEM_ALPHA
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = if (detailed) 78.dp else 64.dp)
            .clickable(enabled = choice.enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (detailed) choice.iconId?.let {
            Icon(painterResource(it), null, Modifier.size(32.dp).alpha(alpha), tint = Color.Unspecified)
            Spacer(Modifier.width(8.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(stringResource(choice.title), color = MaterialTheme.colorScheme.onSurface.copy(alpha), style = MaterialTheme.typography.bodyLarge)
            choice.description?.let {
                Text(
                    stringResource(it),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Icon(
            painterResource(if (choice.enabled) R.drawable.ic_chevron_right else choice.disabledIconId ?: R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha),
        )
    }
}

private const val ENABLED_ITEM_ALPHA = 1f
private const val DISABLED_ITEM_ALPHA = 0.5f

/** Base class for a dialog choice. */
open class DialogChoice(
    @field:StringRes val title: Int,
    @field:StringRes val description: Int? = null,
    @field:DrawableRes val iconId: Int? = null,
    val enabled: Boolean = true,
    @field:DrawableRes val disabledIconId: Int? = null,
)
