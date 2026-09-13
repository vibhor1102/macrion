/* Copyright (C) 2024 Kevin Buzeau — GPLv3 */
package io.github.vibhor1102.macrion.feature.dumb.config.ui.brief

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ElevatedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.vibhor1102.macrion.feature.dumb.config.ui.actions.ActionText
import io.github.vibhor1102.macrion.feature.dumb.config.ui.actions.copy.DumbActionDetails

@Composable
internal fun DumbActionBriefItem(details: DumbActionDetails, orientation: Int, onClick: () -> Unit) {
    val portrait = orientation == Configuration.ORIENTATION_PORTRAIT
    Box(
        modifier = Modifier.fillMaxSize().then(if (portrait) Modifier.padding(horizontal = 16.dp) else Modifier.padding(vertical = 12.dp)),
        contentAlignment = if (portrait) Alignment.BottomCenter else Alignment.CenterStart,
    ) {
        ElevatedCard(
            onClick = onClick,
            modifier = if (portrait) Modifier.fillMaxWidth().height(80.dp) else Modifier.width(124.dp).fillMaxHeight(),
        ) {
            if (portrait) {
                Row(Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    ActionText(details, Modifier.weight(1f).padding(start = 16.dp), Alignment.Start, 1, TextAlign.Start)
                    Image(painterResource(details.icon), null, Modifier.padding(end = 16.dp).size(32.dp))
                }
            } else {
                Column(Modifier.fillMaxSize().padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    ActionText(details, Modifier.weight(1f).fillMaxWidth(), Alignment.CenterHorizontally, 2, TextAlign.Center)
                    Image(painterResource(details.icon), null, Modifier.padding(bottom = 12.dp).size(32.dp))
                }
            }
        }
    }
}
