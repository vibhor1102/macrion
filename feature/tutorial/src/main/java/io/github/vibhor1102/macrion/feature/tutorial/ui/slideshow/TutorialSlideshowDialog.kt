/*
 * Copyright (C) 2026 Kevin Buzeau
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
package io.github.vibhor1102.macrion.feature.tutorial.ui.slideshow

import android.app.Dialog
import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog as ComposeDialog
import io.github.vibhor1102.macrion.core.ui.compose.MacrionDialogSurface
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.core.ui.utils.getDynamicColorsContext
import io.github.vibhor1102.macrion.feature.tutorial.R
import io.github.vibhor1102.macrion.feature.tutorial.data.mapping.toTutorialSlideshow
import io.github.vibhor1102.macrion.feature.tutorial.domain.model.TutorialSlideshow

@Composable
fun TutorialSlideshowDialog(
    slideshowType: TutorialSlideshow.Type,
    pageRange: IntRange? = null,
    onDismiss: () -> Unit,
) {
    val slideshow = remember(slideshowType) { slideshowType.toTutorialSlideshow() }
    val pages = pageRange ?: IntRange(0, slideshow.slideshowItems.lastIndex)
    if (pageRange != null && (pageRange.first < 0 || pageRange.last > slideshow.slideshowItems.lastIndex)) {
        Log.e(TAG, "Can't create slideshow dialog, page range is invalid: $pageRange; " +
                "slideshowItems=${slideshow.slideshowItems.size}")
        return
    }

    ComposeDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            MacrionDialogSurface {
                TutorialSlideshowDialogContent(
                    slideshow = slideshow,
                    pages = slideshow.slideshowItems.subList(pages.first, pages.last + 1),
                    onDismiss = onDismiss,
                )
            }
        }
    }
}

internal fun Context.createTutorialSlideshowDialog(
    slideshowType: TutorialSlideshow.Type,
    pageIndex: Int,
    onDismissed: (() -> Unit)?,
): Dialog? = createDialog(slideshowType.toTutorialSlideshow(), IntRange(pageIndex, pageIndex), onDismissed)

internal fun Context.createTutorialSlideshowDialog(
    slideshowType: TutorialSlideshow.Type,
    pageRange: IntRange? = null,
    onDismissed: (() -> Unit)?,
): Dialog? = createDialog(slideshowType.toTutorialSlideshow(), pageRange, onDismissed)

private fun Context.createDialog(
    slideshow: TutorialSlideshow,
    pageRange: IntRange?,
    onDismissed: (() -> Unit)?,
): Dialog? {

    val pages = pageRange ?: IntRange(0, slideshow.slideshowItems.lastIndex)
    if (pageRange != null && (pageRange.first < 0 || pageRange.last > slideshow.slideshowItems.lastIndex)) {
        Log.e(TAG, "Can't create slideshow dialog, page range is invalid: $pageRange; " +
                "slideshowItems=${slideshow.slideshowItems.size}")
        return null
    }

    val dialogContext = getDynamicColorsContext(R.style.AppTheme)
    lateinit var dialog: Dialog
    val content = ComposeView(dialogContext).apply {
        setContent {
            MacrionTheme {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 6.dp,
                ) {
                    MacrionDialogSurface {
                        TutorialSlideshowDialogContent(
                            slideshow = slideshow,
                            pages = slideshow.slideshowItems.subList(pages.first, pages.last + 1),
                            onDismiss = { dialog.dismiss() },
                        )
                    }
                }
            }
        }
    }
    dialog = Dialog(dialogContext).apply {
        setContentView(content)
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        setOnDismissListener { onDismissed?.invoke() }
    }
    return dialog
}

@Composable
private fun TutorialSlideshowDialogContent(
    slideshow: TutorialSlideshow,
    pages: List<TutorialSlideshow.SlideshowItem>,
    onDismiss: () -> Unit,
) {
    val pagerHeight = (LocalConfiguration.current.screenHeightDp * 0.55f).dp

    TutorialSlideshowContent(
        pages = pages,
        modifier = Modifier.fillMaxWidth(),
        pagerHeight = pagerHeight,
        header = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(slideshow.nameRes),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
                HorizontalDivider()
            }
        },
        onClose = onDismiss,
    )
}

private const val TAG = "TutorialSlideshowDialog"
