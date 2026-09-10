/* Copyright (C) 2024 Kevin Buzeau — GPLv3 */
package io.github.vibhor1102.macrion.feature.smart.config.ui.condition.screen.brief

import android.content.res.Configuration
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.vibhor1102.macrion.core.domain.model.condition.ScreenCondition
import io.github.vibhor1102.macrion.core.ui.utils.setColorIndicatorDrawable
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.model.condition.UiScreenCondition

@Composable
internal fun ScreenConditionBriefItem(details: UiScreenCondition, orientation: Int, onClick: () -> Unit) {
    val portrait = orientation == Configuration.ORIENTATION_PORTRAIT
    Box(Modifier.fillMaxSize().then(if (portrait) Modifier.padding(horizontal = 16.dp) else Modifier.padding(vertical = 16.dp)), if (portrait) Alignment.BottomCenter else Alignment.CenterStart) {
        ElevatedCard(onClick = onClick, modifier = if (portrait) Modifier.fillMaxWidth().height(80.dp) else Modifier.width(124.dp).fillMaxHeight()) {
            Box(Modifier.fillMaxSize()) {
                if (portrait) ConditionPortrait(details) else ConditionLandscape(details)
                if (details.haveError) Box(Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 6.dp).size(6.dp).background(MaterialTheme.colorScheme.error, CircleShape))
            }
        }
    }
}
@Composable private fun ConditionPortrait(details: UiScreenCondition) {
    Row(Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(start = 16.dp, end = 8.dp)) {
            Title(details.name, 1); Spacer(Modifier.height(4.dp)); Row(verticalAlignment = Alignment.CenterVertically) { SubText(details.thresholdText, Modifier.weight(1f)); StateIcon(details); SubText(androidx.compose.ui.res.stringResource(details.shouldBeVisibleTextRes)) }
        }
        VerticalDivider(Modifier.fillMaxHeight().width(1.dp)); ConditionIcon(details, Modifier.padding(horizontal = 16.dp))
    }
}
@Composable private fun ConditionLandscape(details: UiScreenCondition) {
    Column(Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Title(details.name, 2, TextAlign.Center); Spacer(Modifier.height(8.dp)); SubText(details.thresholdText, textAlign = TextAlign.Center); Row(verticalAlignment = Alignment.CenterVertically) { StateIcon(details); SubText(androidx.compose.ui.res.stringResource(details.shouldBeVisibleTextRes)) }; Spacer(Modifier.height(8.dp)); ConditionIcon(details)
    }
}
@Composable private fun Title(text: String, lines: Int, align: TextAlign? = null) = Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = lines, overflow = TextOverflow.Ellipsis, textAlign = align)
@Composable private fun SubText(text: String, modifier: Modifier = Modifier, textAlign: TextAlign? = null) = Text(text, modifier, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = textAlign)
@Composable private fun StateIcon(details: UiScreenCondition) { val tint = MaterialTheme.colorScheme.onPrimaryContainer.toArgb(); AndroidView(factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.FIT_CENTER } }, update = { it.setImageResource(details.shouldBeVisibleIconRes); it.setColorFilter(tint) }, modifier = Modifier.size(16.dp)) }
@Composable private fun ConditionIcon(details: UiScreenCondition, modifier: Modifier = Modifier) { val tint = MaterialTheme.colorScheme.onPrimaryContainer.toArgb(); AndroidView(factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.FIT_CENTER } }, update = { view -> when (val condition = details.condition) { is ScreenCondition.Color -> { view.clearColorFilter(); view.setColorIndicatorDrawable(condition.color) }; is ScreenCondition.Image -> { view.setImageResource(details.detectionTypeIconRes); view.setColorFilter(tint) }; is ScreenCondition.Number -> { view.setImageResource(R.drawable.ic_number_condition); view.setColorFilter(tint) }; is ScreenCondition.Text -> { view.setImageResource(R.drawable.ic_text_condition); view.setColorFilter(tint) } } }, modifier = modifier.size(32.dp)) }
