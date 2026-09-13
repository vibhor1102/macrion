/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.config.ui.counter.creation

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.dialog.OverlayDialog
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTextField
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.core.ui.compose.macrionDoneKeyboardActions
import io.github.vibhor1102.macrion.core.ui.compose.macrionDoneKeyboardOptions
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint

class CounterCreationDialog : OverlayDialog(R.style.ScenarioConfigTheme) {
    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.COUNTER_CREATION.name

    private val viewModel: CountersCreationViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { counterCreationViewModel() },
    )

    override fun onCreateView(): ViewGroup = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { MacrionTheme { this@CounterCreationDialog.Content() } }
    }

@Composable
    private fun Content() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val name by viewModel.name.collectAsStateWithLifecycle()
        var startingValue by rememberSaveable { mutableStateOf("0") }

        Surface(
            Modifier.fillMaxWidth().heightIn(max = 420.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
        ) {
            Column(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = ::back) { Icon(painterResource(R.drawable.ic_cancel), null) }
                    Text(
                        context.getString(R.string.dialog_title_new_counter),
                        Modifier.weight(1f).padding(horizontal = 8.dp),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    FilledIconButton(
                        enabled = state?.canBeSaved == true,
                        onClick = { viewModel.createCounter(); back() },
                    ) { Icon(painterResource(R.drawable.ic_save_filled), null) }
                }
                Column(
                    Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    MacrionTextField(
                        value = name.orEmpty(),
                        onValueChange = viewModel::setName,
                        label = context.getString(R.string.generic_name),
                        isError = state?.nameError == true,
                        maxLength = context.resources.getInteger(R.integer.name_max_length),
                    )
                    if (state?.nameError == true) {
                        Text(
                            context.getString(R.string.field_counter_name_already_declared),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    OutlinedTextField(
                        value = startingValue,
                        onValueChange = { value ->
                            if (value.matches(Regex("-?\\d*(\\.\\d*)?"))) {
                                startingValue = value
                                viewModel.setStartingValue(value.toDoubleOrNull() ?: 0.0)
                            }
                        },
                        label = { Text(context.getString(R.string.field_new_counter_starting_value)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = macrionDoneKeyboardOptions(KeyboardType.Decimal),
                        keyboardActions = macrionDoneKeyboardActions(),
                        singleLine = true,
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}
