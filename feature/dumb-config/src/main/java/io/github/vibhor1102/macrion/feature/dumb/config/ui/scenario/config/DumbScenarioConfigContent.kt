/* Copyright (C) 2023 Kevin Buzeau; Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.dumb.config.ui.scenario.config

import android.content.Context
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vibhor1102.macrion.core.common.overlays.dialog.implementation.navbar.NavBarDialogContent
import io.github.vibhor1102.macrion.core.common.overlays.dialog.implementation.navbar.viewModels
import io.github.vibhor1102.macrion.core.dumb.domain.model.DUMB_SCENARIO_MAX_DURATION_MINUTES
import io.github.vibhor1102.macrion.core.dumb.domain.model.REPEAT_COUNT_MAX_VALUE
import io.github.vibhor1102.macrion.core.dumb.domain.model.REPEAT_COUNT_MIN_VALUE
import io.github.vibhor1102.macrion.core.ui.compose.MacrionAnimatedDescription
import io.github.vibhor1102.macrion.core.ui.compose.macrionDoneKeyboardActions
import io.github.vibhor1102.macrion.core.ui.compose.macrionDoneKeyboardOptions
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.feature.dumb.config.R
import io.github.vibhor1102.macrion.feature.dumb.config.di.DumbConfigViewModelsEntryPoint

class DumbScenarioConfigContent(appContext: Context) : NavBarDialogContent(appContext) {

    private val viewModel: DumbScenarioConfigContentViewModel by viewModels(
        entryPoint = DumbConfigViewModelsEntryPoint::class.java,
        creator = { dumbScenarioConfigContentViewModel() },
    )

    override fun onCreateView(container: ViewGroup): ViewGroup = ComposeView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { MacrionTheme { this@DumbScenarioConfigContent.Content() } }
    }

    override fun onViewCreated() = Unit

    @Composable
    private fun Content() {
        val initialName by viewModel.scenarioName.collectAsStateWithLifecycle(initialValue = null)
        val initialRepeat by viewModel.repeatCount.collectAsStateWithLifecycle(initialValue = null)
        val initialDuration by viewModel.maxDurationMin.collectAsStateWithLifecycle(initialValue = null)
        val nameError by viewModel.scenarioNameError.collectAsStateWithLifecycle(initialValue = false)
        val repeatError by viewModel.repeatCountError.collectAsStateWithLifecycle(initialValue = false)
        val durationError by viewModel.maxDurationMinError.collectAsStateWithLifecycle(initialValue = false)
        val repeatInfinite by viewModel.repeatInfiniteState.collectAsStateWithLifecycle(initialValue = true)
        val durationInfinite by viewModel.maxDurationMinInfiniteState.collectAsStateWithLifecycle(initialValue = true)
        val randomization by viewModel.randomization.collectAsStateWithLifecycle(initialValue = false)
        val antiDetectionEnabledDescription = stringResource(R.string.dropdown_helper_text_anti_detection_enabled)
        val antiDetectionDisabledDescription = stringResource(R.string.dropdown_helper_text_anti_detection_disabled)

        var name by rememberSaveable { mutableStateOf("") }
        var repeatCount by rememberSaveable { mutableStateOf("") }
        var maxDuration by rememberSaveable { mutableStateOf("") }
        var nameFocused by remember { mutableStateOf(false) }
        var repeatFocused by remember { mutableStateOf(false) }
        var durationFocused by remember { mutableStateOf(false) }
        LaunchedEffect(initialName, nameFocused) { if (!nameFocused) initialName?.let { name = it } }
        LaunchedEffect(initialRepeat, repeatFocused) { if (!repeatFocused) initialRepeat?.let { repeatCount = it } }
        LaunchedEffect(initialDuration, durationFocused) { if (!durationFocused) initialDuration?.let { maxDuration = it } }

        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { value ->
                        val sanitized = value.take(context.resources.getInteger(R.integer.name_max_length))
                        name = sanitized
                        viewModel.setDumbScenarioName(sanitized)
                    },
                    modifier = Modifier.fillMaxWidth().onFocusChanged { nameFocused = it.isFocused },
                    label = { Text(stringResource(R.string.input_field_label_scenario_name)) },
                    isError = nameError,
                    singleLine = true,
                    keyboardOptions = macrionDoneKeyboardOptions(),
                    keyboardActions = macrionDoneKeyboardActions(),
                    trailingIcon = if (name.isNotEmpty()) {{
                        IconButton({ name = ""; viewModel.setDumbScenarioName("") }) {
                            Icon(painterResource(R.drawable.ic_cancel), null)
                        }
                    }} else null,
                )
                Spacer(Modifier.height(8.dp))
                InfiniteNumberField(
                    repeatCount, stringResource(R.string.input_field_label_repeat_count), repeatInfinite,
                    repeatError, REPEAT_COUNT_MIN_VALUE, REPEAT_COUNT_MAX_VALUE,
                    { repeatFocused = it },
                    { repeatCount = it; viewModel.setRepeatCount(it.toIntOrNull() ?: 0) },
                    viewModel::toggleInfiniteRepeat,
                )
                Spacer(Modifier.height(8.dp))
                InfiniteNumberField(
                    maxDuration, stringResource(R.string.input_field_label_maximum_duration), durationInfinite,
                    durationError, 1, DUMB_SCENARIO_MAX_DURATION_MINUTES,
                    { durationFocused = it },
                    { maxDuration = it; viewModel.setMaxDurationMinutes(it.toIntOrNull() ?: 0) },
                    viewModel::toggleInfiniteMaxDuration,
                )
                Spacer(Modifier.height(12.dp))
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().clickable(role = Role.Switch, onClick = viewModel::toggleRandomization)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            Modifier.weight(1f).padding(end = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(stringResource(R.string.input_field_label_anti_detection), style = MaterialTheme.typography.bodyLarge)
                            MacrionAnimatedDescription(
                                text = if (randomization) antiDetectionEnabledDescription else antiDetectionDisabledDescription,
                            )
                        }
                        VerticalDivider(Modifier.height(48.dp))
                        Spacer(Modifier.width(12.dp))
                        Switch(randomization, { viewModel.toggleRandomization() })
                    }
                }
            }
        }
    }

    @Composable
    private fun InfiniteNumberField(
        value: String,
        label: String,
        checked: Boolean,
        isError: Boolean,
        min: Int,
        max: Int,
        onFocusChanged: (Boolean) -> Unit,
        onValueChange: (String) -> Unit,
        onToggle: () -> Unit,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            OutlinedTextField(
                value,
                { candidate ->
                    if (candidate.isEmpty() || candidate.toIntOrNull()?.let { it in min..max } == true) {
                        onValueChange(candidate)
                    }
                },
                Modifier.weight(1f).onFocusChanged { onFocusChanged(it.isFocused) },
                label = { Text(label) },
                enabled = !checked,
                isError = isError,
                keyboardOptions = macrionDoneKeyboardOptions(KeyboardType.Number),
                keyboardActions = macrionDoneKeyboardActions(),
                singleLine = true,
                trailingIcon = if (value.isNotEmpty() && !checked) {{
                    IconButton({ onValueChange("") }) { Icon(painterResource(R.drawable.ic_cancel), null) }
                }} else null,
            )
            Spacer(Modifier.width(16.dp))
            OutlinedIconToggleButton(
                checked = checked,
                onCheckedChange = { onToggle() },
                modifier = Modifier.padding(top = 8.dp).size(48.dp),
            ) {
                Icon(painterResource(R.drawable.ic_infinite), label, Modifier.size(24.dp))
            }
        }
    }
}
