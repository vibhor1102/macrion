/* Copyright (C) 2026 Kevin Buzeau; Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.config

import android.content.Context
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vibhor1102.macrion.core.common.overlays.dialog.implementation.navbar.NavBarDialogContent
import io.github.vibhor1102.macrion.core.common.overlays.dialog.implementation.navbar.viewModels
import io.github.vibhor1102.macrion.core.ui.compose.MacrionAnimatedDescription
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTextField
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.core.ui.compose.macrionDoneKeyboardActions
import io.github.vibhor1102.macrion.core.ui.compose.macrionDoneKeyboardOptions
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.formatters.toNaturalDisplayString
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
class ScenarioConfigContent(appContext: Context) : NavBarDialogContent(appContext) {
    private val viewModel: ScenarioConfigViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { scenarioConfigViewModel() },
    )

    override fun onCreateView(container: ViewGroup): ViewGroup = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { MacrionTheme { this@ScenarioConfigContent.Content() } }
    }

    override fun onViewCreated() = Unit

    @Composable
    private fun Content() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
            state?.let {
                val antiDetectionEnabledDescription = stringResource(R.string.dropdown_helper_text_anti_detection_enabled)
                val antiDetectionDisabledDescription = stringResource(R.string.dropdown_helper_text_anti_detection_disabled)
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ScenarioName(it.name)
                    SwitchCard(
                        stringResource(R.string.input_field_label_anti_detection),
                        if (it.randomizeChecked) antiDetectionEnabledDescription else antiDetectionDisabledDescription,
                        it.randomizeChecked,
                        viewModel::toggleRandomization,
                        animateDescription = true,
                    )
                    SwitchCard(
                        stringResource(R.string.field_scenario_keep_screen_on_title),
                        stringResource(if (it.keepScreenOnChecked) R.string.field_scenario_keep_screen_on_enabled
                        else R.string.field_scenario_keep_screen_on_disabled),
                        it.keepScreenOnChecked,
                        viewModel::toggleKeepScreenOn,
                    )
                    ComputeRateCard(it.computeRateState)
                    DetectionQualityCard(it.qualityUiState)
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }

    @Composable
    private fun ScenarioName(repositoryName: String) {
        var name by rememberSaveable { mutableStateOf(repositoryName) }
        var focused by remember { mutableStateOf(false) }
        LaunchedEffect(repositoryName, focused) { if (!focused) name = repositoryName }
        val maxLength = context.resources.getInteger(R.integer.name_max_length)
        MacrionTextField(
            value = name,
            onValueChange = { value -> name = value; viewModel.setScenarioName(value) },
            modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
            label = stringResource(R.string.input_field_label_scenario_name),
            maxLength = maxLength,
        )
    }

    @Composable
    private fun SwitchCard(
        title: String,
        description: String,
        checked: Boolean,
        onToggle: () -> Unit,
        animateDescription: Boolean = false,
    ) {
        ElevatedCard(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f).padding(end = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, style = MaterialTheme.typography.bodyLarge)
                    if (animateDescription) MacrionAnimatedDescription(description)
                    else Text(description, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                VerticalDivider(Modifier.height(48.dp))
                Spacer(Modifier.width(12.dp))
                Switch(checked, { onToggle() })
            }
        }
    }

    @Composable
    private fun ComputeRateCard(state: ComputeRateLimitUiState) {
        var value by rememberSaveable(state.value) { mutableStateOf(state.value.toNaturalDisplayString()) }
        var focused by remember { mutableStateOf(false) }
        var menuExpanded by remember { mutableStateOf(false) }
        androidx.compose.runtime.LaunchedEffect(menuExpanded) {
            io.github.vibhor1102.macrion.core.base.crash.CrashDiagnostics.record(
                if (menuExpanded) io.github.vibhor1102.macrion.core.base.crash.CrashDiagnostics.Event.DROPDOWN_OPENED
                else io.github.vibhor1102.macrion.core.base.crash.CrashDiagnostics.Event.DROPDOWN_CLOSED,
                "io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.config.ScenarioConfigContent",
            )
        }
        LaunchedEffect(state.value, state.unit, state.isEnabled, focused) {
            if (!focused && state.isEnabled) value = state.value.toNaturalDisplayString()
        }
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(
                    Modifier.fillMaxWidth().clickable(onClick = viewModel::toggleFpsLimiter),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f).padding(end = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.field_scenario_fps_limit_title), style = MaterialTheme.typography.bodyLarge)
                        Text(stringResource(R.string.field_scenario_fps_limit_desc), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    VerticalDivider(Modifier.height(48.dp))
                    Spacer(Modifier.width(12.dp))
                    Switch(state.isEnabled, { viewModel.toggleFpsLimiter() })
                }
                AnimatedVisibility(
                    visible = state.isEnabled,
                    enter = expandVertically(
                        animationSpec = spring(
                            dampingRatio = 0.85f,
                            stiffness = Spring.StiffnessMedium,
                        ),
                        expandFrom = Alignment.Top,
                    ) + fadeIn(
                        animationSpec = tween(150),
                    ),
                    exit = shrinkVertically(
                        animationSpec = spring(
                            dampingRatio = 0.85f,
                            stiffness = Spring.StiffnessMedium,
                        ),
                        shrinkTowards = Alignment.Top,
                    ) + fadeOut(
                        animationSpec = tween(100),
                    ),
                ) {
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value,
                            { input ->
                                if (input.matches(Regex("\\d*(\\.\\d*)?"))) {
                                    value = input
                                    input.toDoubleOrNull()?.takeIf { it > FRAME_LIMIT_MIN_VALUE && it <= state.maxValue }
                                        ?.let(viewModel::setComputeRate)
                                }
                            },
                            Modifier.weight(1f).onFocusChanged { focused = it.isFocused },
                            label = { Text(stringResource(R.string.field_scenario_fps_rate_label)) },
                            singleLine = true,
                            keyboardOptions = macrionDoneKeyboardOptions(KeyboardType.Decimal),
                            keyboardActions = macrionDoneKeyboardActions(),
                        )
                        Text("/", Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.headlineMedium)
                        ExposedDropdownMenuBox(menuExpanded, { menuExpanded = !menuExpanded }, Modifier.weight(.8f)) {
                            OutlinedTextField(
                                stringResource(state.unit.title), {}, readOnly = true,
                                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                                label = { Text(stringResource(R.string.field_scenario_fps_rate_unit_label)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(menuExpanded) },
                                singleLine = true,
                            )
                            ExposedDropdownMenu(menuExpanded, { menuExpanded = false }) {
                                allComputeRateUnitDropdownItems().forEach { item ->
                                    DropdownMenuItem(
                                        { Text(stringResource(item.title)) },
                                        { viewModel.setComputeRateUnit(item); menuExpanded = false },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun DetectionQualityCard(state: DetectionQualityUiState) {
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(stringResource(R.string.field_scenario_quality_title), style = MaterialTheme.typography.bodyLarge)
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilledTonalIconButton(viewModel::decreaseDetectionQuality, Modifier.size(48.dp)) {
                        Icon(painterResource(R.drawable.ic_chevron_right), null, Modifier.rotate(180f))
                    }
                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) {
                        Text(state.displayText, Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer, style = MaterialTheme.typography.bodyLarge)
                    }
                    FilledTonalIconButton(viewModel::increaseDetectionQuality, Modifier.size(48.dp)) {
                        Icon(painterResource(R.drawable.ic_chevron_right), null)
                    }
                }
                Slider(
                    value = state.qualityValue,
                    onValueChange = { viewModel.setDetectionQuality(it.roundToInt()) },
                    modifier = Modifier.fillMaxWidth(),
                    valueRange = state.min..state.max,
                )
                HorizontalDivider()
                Text(
                    stringResource(R.string.field_scenario_quality_description),
                    Modifier.padding(top = 12.dp, bottom = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
