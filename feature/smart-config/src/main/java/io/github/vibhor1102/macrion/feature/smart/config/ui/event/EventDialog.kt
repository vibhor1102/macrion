/* Copyright (C) 2024 Kevin Buzeau; Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.config.ui.event

import android.util.Log
import android.view.ViewGroup
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.dialog.OverlayDialog
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredViewType
import io.github.vibhor1102.macrion.core.domain.model.AND
import io.github.vibhor1102.macrion.core.domain.model.OR
import io.github.vibhor1102.macrion.core.domain.model.condition.ScreenCondition
import io.github.vibhor1102.macrion.core.ui.bindings.dropdown.TimeUnitDropDownItem
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTextField
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.core.ui.compose.macrionDoneKeyboardActions
import io.github.vibhor1102.macrion.core.ui.compose.macrionDoneKeyboardOptions
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import io.github.vibhor1102.macrion.feature.smart.config.ui.action.brief.SmartActionsBriefMenu
import io.github.vibhor1102.macrion.feature.smart.config.ui.action.brief.SmartActionsLegacyDialog
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.compose.LocalMonitoredViewsManager
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.compose.tutorialAnchor
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.formatters.toEffectDescription
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.formatters.toNaturalDisplayString
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.dialogs.showCloseWithoutSavingDialog
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.dialogs.showDeleteEventWithAssociatedActionsDialog
import io.github.vibhor1102.macrion.feature.smart.config.ui.condition.screen.brief.ScreenConditionsBriefMenu
import io.github.vibhor1102.macrion.feature.smart.config.ui.condition.trigger.TriggerConditionListDialog
import io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.live.eventtry.TryEventOverlayMenu
import kotlinx.coroutines.launch

class EventDialog(private val onConfigComplete: () -> Unit, private val onDelete: () -> Unit,
    private val onDismiss: () -> Unit) : OverlayDialog(R.style.ScenarioConfigTheme) {
    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.EVENT.name
    private val viewModel: EventDialogViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java, creator = { eventDialogViewModel() })

    override fun onCreateView(): ViewGroup = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { MacrionTheme { this@EventDialog.Content() } }
    }
    override fun onDialogCreated(dialog: BottomSheetDialog) {
        lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.CREATED) {
            viewModel.isEditingEvent.collect { if (!it) { Log.e(TAG, "Closing EventDialog because no event is edited"); finish() } }
        } }
    }

    @Composable private fun Content() {
        CompositionLocalProvider(LocalMonitoredViewsManager provides viewModel.monitoredViewsManager) {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val ui = state ?: return@CompositionLocalProvider
            var name by rememberSaveable { mutableStateOf(ui.name.orEmpty()) }
            LaunchedEffect(ui.name) { if (name != ui.name) name = ui.name.orEmpty() }
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
                Column { TopBar(ui.canBeSaved)
                    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        MacrionTextField(name, { name = it; viewModel.setEventName(it) }, context.getString(R.string.generic_name),
                            isError = ui.nameError, maxLength = context.resources.getInteger(R.integer.name_max_length))
                        ConditionsCard(ui)
                        ActionsCard(ui.actionsItems)
                        StateCard(ui)
                        if (ui is EventDialogUiState.ScreenEvent) TestCard(ui.canTryEvent)
                    }
                }
            }
        }
    }

    @Composable private fun TopBar(enabled: Boolean) { Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = ::back) { Icon(painterResource(R.drawable.ic_cancel), null) }
        Text(context.getString(if (viewModel.isConfiguringScreenEvent()) R.string.dialog_title_image_event else R.string.dialog_title_trigger_event),
            Modifier.weight(1f).padding(horizontal = 8.dp), style = MaterialTheme.typography.titleLarge,
            maxLines = 1, overflow = TextOverflow.Clip)
        FilledTonalIconButton(onClick = ::delete) { Icon(painterResource(R.drawable.ic_delete), null) }
        Spacer(Modifier.width(8.dp))
        FilledIconButton(
            onClick = ::save,
            enabled = enabled,
            modifier = Modifier.tutorialAnchor(
                MonitoredViewType.EVENT_DIALOG_BUTTON_SAVE,
                onClick = ::save,
                enabled = enabled,
            ),
        ) { Icon(painterResource(R.drawable.ic_save_filled), null) }
    } }

    @Composable private fun ConditionsCard(ui: EventDialogUiState) {
        EventCard(context.getString(R.string.menu_item_title_conditions), colorResource(R.color.event_conditions_color)) {
            Box(Modifier.tutorialAnchor(
                MonitoredViewType.EVENT_DIALOG_FIELD_CONDITIONS,
                onClick = ::showConditions,
            )) {
                when (ui) {
                    is EventDialogUiState.ScreenEvent -> ScreenConditionSelector(ui.imageConditionsItems)
                    is EventDialogUiState.TriggerEvent -> ChildrenSelector(ui.triggerConditionsItems, true, ::showConditions)
                }
            }
            HorizontalDivider()
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(end = 16.dp)) { Text(context.getString(R.string.field_operator_title), style = MaterialTheme.typography.bodyLarge)
                    Text(context.getString(if (ui.conditionOperator == AND) R.string.field_operator_desc_and else R.string.field_operator_desc_or),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                VerticalDivider(Modifier.height(48.dp))
                Spacer(Modifier.width(12.dp))
                OperatorButtons(ui.conditionOperator)
            }
        }
    }

    @Composable private fun ActionsCard(items: List<EventChildrenItem>) = EventCard(
        context.getString(R.string.menu_item_title_actions), colorResource(R.color.event_actions_color)) {
        Box(Modifier.tutorialAnchor(
            MonitoredViewType.EVENT_DIALOG_FIELD_ACTIONS,
            onClick = { showActionsOverlay() },
        )) {
            ChildrenSelector(items, false, ::showActionsOverlay)
        }
    }

    @Composable private fun EventCard(title: String, accent: Color, content: @Composable ColumnScope.() -> Unit) {
        ElevatedCard(Modifier.fillMaxWidth().border(2.dp, accent, RoundedCornerShape(12.dp))) {
            Text(title, Modifier.fillMaxWidth().background(accent).padding(horizontal = 16.dp, vertical = 6.dp),
                style = MaterialTheme.typography.bodyLarge, color = Color.White, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), content = content)
        }
    }

    @Composable private fun ScreenConditionSelector(items: List<io.github.vibhor1102.macrion.feature.smart.config.ui.common.model.condition.UiScreenCondition>) {
        if (items.isEmpty()) { EmptySelector(true, ::showConditions); return }
        Row(Modifier.fillMaxWidth().height(116.dp), verticalAlignment = Alignment.CenterVertically) {
            LazyRow(Modifier.weight(1f).fillMaxHeight()) {
                itemsIndexed(items, key = { _, item -> item.condition.id.databaseId.takeIf { it != 0L } ?: -requireNotNull(item.condition.id.tempId) }) { index, item ->
                    EventImageConditionCard(item, viewModel::getConditionBitmap) { showImageConditionsBriefMenu(index) }
                }
            }
            Spacer(Modifier.width(8.dp))
            FilledTonalIconButton(onClick = ::showConditions, modifier = Modifier.size(40.dp)) {
                Icon(painterResource(R.drawable.ic_chevron_right), null)
            }
        }
    }

    @Composable private fun ChildrenSelector(items: List<EventChildrenItem>, conditions: Boolean, onClick: () -> Unit) {
        if (items.isEmpty()) { EmptySelector(conditions, onClick); return }
        Row(Modifier.fillMaxWidth().heightIn(min = 62.dp).padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            LazyRow(Modifier.weight(1f).height(64.dp)) {
                itemsIndexed(items) { index, item ->
                    EventChildCard(item) { if (conditions) showTriggerConditionsDialog() else showActionsOverlay(index) }
                }
            }
            Spacer(Modifier.width(8.dp))
            FilledTonalIconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
                Icon(painterResource(R.drawable.ic_chevron_right), null)
            }
        }
    }

    @Composable private fun EmptySelector(conditions: Boolean, onClick: () -> Unit) {
        Row(Modifier.fillMaxWidth().heightIn(min = 62.dp).clickable(onClick = onClick).padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(context.getString(if (conditions) { if (viewModel.isConfiguringScreenEvent()) R.string.message_empty_screen_condition_list_title
                    else R.string.message_empty_trigger_condition_list_title } else R.string.message_empty_action_list_title),
                    style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(context.getString(if (conditions) { if (viewModel.isConfiguringScreenEvent()) R.string.message_empty_screen_condition_list_desc
                    else R.string.message_empty_trigger_condition_list_desc } else R.string.message_empty_action_list_desc),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            Icon(painterResource(R.drawable.ic_chevron_right), null)
        }
    }

    @Composable private fun OperatorButtons(selected: Int) { val shape = RoundedCornerShape(18.dp)
        Row(Modifier.height(32.dp).clip(shape).border(1.dp, MaterialTheme.colorScheme.outline, shape)) {
            listOf(AND to R.string.condition_operator_and, OR to R.string.condition_operator_or).forEachIndexed { index, pair ->
                if (index > 0) Box(Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outline))
                val anchorType = if (pair.first == AND) MonitoredViewType.EVENT_DIALOG_FIELD_OPERATOR_ITEM_AND else MonitoredViewType.EVENT_DIALOG_FIELD_OPERATOR_ITEM_OR
                Box(Modifier.width(48.dp).fillMaxHeight().background(if (selected == pair.first) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                    .clickable { viewModel.setConditionOperator(pair.first) }
                    .tutorialAnchor(anchorType, onClick = { viewModel.setConditionOperator(pair.first) }),
                    contentAlignment = Alignment.Center) {
                    Text(stringResource(pair.second), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }

    @Composable private fun StateCard(ui: EventDialogUiState) = EventCard(context.getString(R.string.menu_item_title_state),
        colorResource(R.color.event_state_color)) {
        SwitchField(
            context.getString(R.string.field_event_state_title),
            context.getString(if (ui.enabledOnStart) R.string.field_event_state_desc_enabled else R.string.field_event_state_desc_disabled),
            ui.enabledOnStart,
            viewModel::toggleEventState,
            modifier = Modifier.tutorialAnchor(MonitoredViewType.EVENT_DIALOG_FIELD_INITIAL_STATE, onClick = viewModel::toggleEventState),
        )
        if (ui is EventDialogUiState.ScreenEvent) { HorizontalDivider(); SwitchField(context.getString(R.string.field_event_keep_detecting_title),
            context.getString(if (ui.keepDetecting) R.string.field_event_keep_detecting_desc_enabled else R.string.field_event_keep_detecting_desc_disabled),
            ui.keepDetecting, viewModel::toggleKeepDetectingState); HorizontalDivider(); CooldownField(ui) }
    }

    @Composable private fun SwitchField(title: String, desc: String, checked: Boolean, toggle: () -> Unit, modifier: Modifier = Modifier) {
        Row(Modifier.fillMaxWidth().then(modifier).clickable(onClick = toggle).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 16.dp)) { Text(title, style = MaterialTheme.typography.bodyLarge); Text(desc,
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            VerticalDivider(Modifier.height(48.dp))
            Spacer(Modifier.width(12.dp))
            Switch(checked, { toggle() })
        }
    }

    @Composable private fun CooldownField(ui: EventDialogUiState.ScreenEvent) { var value by rememberSaveable { mutableStateOf(ui.cooldownValue) }
        LaunchedEffect(ui.cooldownValue) { if (value != ui.cooldownValue) value = ui.cooldownValue }
        SwitchField(context.getString(R.string.field_event_cooldown_title), context.getString(R.string.field_event_cooldown_desc),
            ui.cooldownEnabled, viewModel::toggleCooldownState)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            OutlinedTextField(value, { text -> val filtered = text.filter(Char::isDigit); value = filtered
                viewModel.setCooldownValue(filtered.toLongOrNull()) }, Modifier.weight(.7f), enabled = ui.cooldownEnabled,
                label = { Text(context.getString(R.string.field_event_cooldown_edit_label)) }, singleLine = true,
                keyboardOptions = macrionDoneKeyboardOptions(KeyboardType.Number),
                keyboardActions = macrionDoneKeyboardActions())
            Spacer(Modifier.width(12.dp)); TimeUnitDropdown(ui.cooldownUnit, ui.cooldownEnabled, Modifier.weight(.3f))
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable private fun TimeUnitDropdown(unit: TimeUnitDropDownItem, enabled: Boolean, modifier: Modifier) { var expanded by remember { mutableStateOf(false) }
        val units = listOf(TimeUnitDropDownItem.Milliseconds, TimeUnitDropDownItem.Seconds, TimeUnitDropDownItem.Minutes)
        ExposedDropdownMenuBox(expanded, { if (enabled) expanded = it }, modifier) {
            OutlinedTextField(stringResource(unit.title), {}, Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                enabled = enabled, readOnly = true, label = { Text(context.getString(R.string.dropdown_label_time_unit)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) })
            ExposedDropdownMenu(expanded, { expanded = false }) { units.forEach { item -> DropdownMenuItem({ Text(stringResource(item.title)) },
                { viewModel.setCooldownTimeUnit(item); expanded = false }) } }
        }
    }

    @Composable private fun TestCard(enabled: Boolean) {
        ElevatedCard(Modifier.fillMaxWidth().border(2.dp, colorResource(R.color.event_test_color), RoundedCornerShape(12.dp))) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(context.getString(R.string.item_title_try_element, context.getString(R.string.dialog_title_image_event)),
                    Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                FilledIconButton(::showTryElementMenu, enabled = enabled) {
                    Icon(painterResource(R.drawable.ic_play_arrow), null, Modifier.size(18.dp))
                }
            }
        }
    }

    override fun back() { if (viewModel.hasUnsavedModifications()) { context.showCloseWithoutSavingDialog { onDismiss(); super.back() }; return }
        onDismiss(); super.back() }
    private fun save() { onConfigComplete(); super.back() }
    private fun delete() { if (viewModel.isEventHaveRelatedActions()) context.showDeleteEventWithAssociatedActionsDialog { confirmDelete() } else confirmDelete() }
    private fun confirmDelete() { onDelete(); super.back() }
    private fun showConditions() { if (viewModel.isConfiguringScreenEvent()) showImageConditionsBriefMenu() else showTriggerConditionsDialog() }
    private fun showImageConditionsBriefMenu(index: Int = 0) = overlayManager.navigateTo(context, ScreenConditionsBriefMenu(index), true)
    private fun showTriggerConditionsDialog() = overlayManager.navigateTo(context, TriggerConditionListDialog())
    private fun showActionsOverlay(index: Int = 0) = overlayManager.navigateTo(context,
        if (viewModel.isLegacyActionUiEnabled()) SmartActionsLegacyDialog() else SmartActionsBriefMenu(index), true)
    private fun showTryElementMenu() { viewModel.getTryInfo()?.let { (scenario, event) ->
        overlayManager.navigateTo(context, TryEventOverlayMenu(scenario, event), true) } }
}

@Composable
private fun EventImageConditionCard(
    uiCondition: io.github.vibhor1102.macrion.feature.smart.config.ui.common.model.condition.UiScreenCondition,
    bitmapProvider: (ScreenCondition.Image, (Bitmap?) -> Unit) -> kotlinx.coroutines.Job?,
    onClick: () -> Unit,
) {
    var bitmap by remember(uiCondition.condition.id) { mutableStateOf<Bitmap?>(null) }
    var bitmapFailed by remember(uiCondition.condition.id) { mutableStateOf(false) }
    val imageCondition = uiCondition.condition as? ScreenCondition.Image
    DisposableEffect(imageCondition) {
        val loadingJob = imageCondition?.let { image -> bitmapProvider(image) { loaded -> bitmap = loaded; bitmapFailed = loaded == null } }
        onDispose { loadingJob?.cancel() }
    }
    Box(Modifier.size(108.dp).padding(horizontal = 4.dp, vertical = 4.dp)) {
        OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxSize(), elevation = CardDefaults.outlinedCardElevation(defaultElevation = 2.dp)) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth().padding(vertical = 2.dp).clipToBounds().background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                    when (val condition = uiCondition.condition) {
                        is ScreenCondition.Color -> ColorIndicator(condition.color)
                        is ScreenCondition.Image -> when {
                            bitmap != null -> androidx.compose.foundation.Image(bitmap!!.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Fit)
                            bitmapFailed -> Icon(painterResource(R.drawable.ic_cancel), null, tint = MaterialTheme.colorScheme.error)
                        }
                        is ScreenCondition.Number -> Text(condition.comparisonOperation.toEffectDescription(LocalContext.current, operand = condition.counterValue.toNaturalDisplayString()), Modifier.padding(horizontal = 4.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        is ScreenCondition.Text -> Text(condition.text, Modifier.padding(horizontal = 4.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
                HorizontalDivider()
                Text(uiCondition.name, Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.bodySmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(Modifier.fillMaxWidth().height(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Icon(painterResource(uiCondition.shouldBeVisibleIconRes), null, Modifier.height(16.dp)) }
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { if (imageCondition != null) Icon(painterResource(uiCondition.detectionTypeIconRes), null, Modifier.height(16.dp)) }
                    Text(uiCondition.thresholdText, Modifier.weight(1f), fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun ColorIndicator(color: Int) {
    val border = MaterialTheme.colorScheme.onSurfaceVariant
    androidx.compose.foundation.Canvas(Modifier.size(48.dp)) {
        drawCircle(Color(color), radius = 20.dp.toPx(), center = center)
        drawCircle(border, radius = 22.dp.toPx(), center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(4.dp.toPx()))
    }
}

@Composable
private fun EventChildCard(item: EventChildrenItem, onClick: () -> Unit) {
    Box(Modifier.width(56.dp).height(64.dp).padding(horizontal = 4.dp, vertical = 8.dp)) {
        Surface(Modifier.fillMaxSize().border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium).clickable(onClick = onClick), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerLow, shadowElevation = 2.dp) {
            Box(contentAlignment = Alignment.Center) {
                Icon(painterResource(item.iconRes), null, Modifier.size(29.dp))
                if (item.isInError) Box(Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 6.dp).size(6.dp).background(MaterialTheme.colorScheme.error, CircleShape))
            }
        }
    }
}
private const val TAG = "EventDialog"
