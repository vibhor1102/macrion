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
package io.github.vibhor1102.macrion.core.common.overlays.menu.implementation.brief

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityManager
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import io.github.vibhor1102.macrion.core.base.identifier.Identifier
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.graphicsLayer
import io.github.vibhor1102.macrion.core.common.overlays.R
import io.github.vibhor1102.macrion.core.display.config.DisplayConfig
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.core.ui.compose.overlay.GestureRecordOverlay
import io.github.vibhor1102.macrion.core.ui.compose.overlay.ItemBriefCanvas
import io.github.vibhor1102.macrion.core.ui.views.gesturerecord.RecordedGesture
import io.github.vibhor1102.macrion.core.ui.views.itembrief.ItemBriefDescription
import io.github.vibhor1102.macrion.core.ui.R as UiR

interface ItemBriefViewFacade {
    fun setDescription(newDescription: ItemBriefDescription?, animate: Boolean = true)
}

interface GestureRecordViewFacade {
    var isVisible: Boolean
    var gestureCaptureListener: ((gesture: RecordedGesture?, isFinished: Boolean) -> Unit)?
    fun clearAndHide()
}

class ItemsBriefOverlayViewBinding private constructor(
    val root: ComposeView,
    initialOrientation: Int,
    initialDisplayConfig: DisplayConfig,
) {
    var orientation by mutableIntStateOf(initialOrientation)
        private set
    var displayConfig by mutableStateOf(initialDisplayConfig)
        private set

    val currentDescription = mutableStateOf<ItemBriefDescription?>(null)
    private val isAnimateEnabled = mutableStateOf(true)
    private var internalGestureCaptureListener: ((gesture: RecordedGesture?, isFinished: Boolean) -> Unit)? = null

    val viewBrief: ItemBriefViewFacade = object : ItemBriefViewFacade {
        override fun setDescription(newDescription: ItemBriefDescription?, animate: Boolean) {
            currentDescription.value = newDescription
            isAnimateEnabled.value = animate
        }
    }

    val viewRecorder: GestureRecordViewFacade = object : GestureRecordViewFacade {
        override var isVisible: Boolean
            get() = isGestureRecording.value
            set(value) { isGestureRecording.value = value }

        override var gestureCaptureListener: ((gesture: RecordedGesture?, isFinished: Boolean) -> Unit)?
            get() = internalGestureCaptureListener
            set(value) { internalGestureCaptureListener = value }

        override fun clearAndHide() {
            isGestureRecording.value = false
            internalGestureCaptureListener = null
        }
    }

    private val emptyText = mutableIntStateOf(0)
    private val controlState = mutableStateOf(ItemBriefControlsState())
    private val briefItems = mutableStateOf<List<ItemBrief>>(emptyList())
    private val requestedBriefItemIndex = mutableIntStateOf(0)
    private val isPanelVisible = mutableStateOf(false)
    private val isPointerActive = mutableStateOf(false)
    private val isCarouselScrolling = mutableStateOf(false)
    val isGestureRecording = mutableStateOf(false)
    private val isInstructionsVisible = mutableStateOf(false)
    private val isPanelAutoHideEnabled = mutableStateOf(true)

    private val panelTimerTrigger = mutableIntStateOf(0)
    private val instructionsTimerTrigger = mutableIntStateOf(0)

    private var briefItemContent: (@Composable (ItemBrief, Int, () -> Unit) -> Unit)? = null
    private var onItemClicked: (Int, ItemBrief) -> Unit = { _, _ -> }
    private var onFocusedItemChanged: (Int) -> Unit = {}
    private var firstItemModifier by mutableStateOf<@Composable () -> Modifier>({ Modifier })

    private var onDelete: () -> Unit = {}
    private var onPosition: () -> Unit = {}
    private var onPlay: () -> Unit = {}
    private var onReorder: () -> Unit = {}

    companion object {

        private const val PANEL_EXIT_DURATION_MS = 600
        private const val INSTRUCTIONS_HIDE_DELAY_MS = 3_000L

        fun inflate(
            inflater: LayoutInflater,
            orientation: Int,
            displayConfig: DisplayConfig,
        ): ItemsBriefOverlayViewBinding {
            val context = inflater.context
            val root = ComposeView(context)
            return ItemsBriefOverlayViewBinding(root, orientation, displayConfig).apply {
                root.setContent { MacrionTheme { OverlayContent() } }
            }
        }
    }

    fun setEmptyText(textRes: Int) {
        emptyText.intValue = textRes
    }

    fun setControlCallbacks(
        onDelete: () -> Unit,
        onPosition: () -> Unit,
        onPlay: () -> Unit,
        onReorder: () -> Unit,
    ) {
        this.onDelete = onDelete
        this.onPosition = onPosition
        this.onPlay = onPlay
        this.onReorder = onReorder
    }

    fun scrollToItem(index: Int) {
        requestedBriefItemIndex.intValue = index
        showOrResetPanelTimer()
    }

    fun updateControls(state: ItemBriefControlsState) {
        controlState.value = state
    }

    fun setBriefItemsContent(
        initialItemIndex: Int,
        itemContent: @Composable (ItemBrief, Int, () -> Unit) -> Unit,
        onItemClicked: (Int, ItemBrief) -> Unit,
        onFocusedItemChanged: (Int) -> Unit,
        firstItemModifier: @Composable () -> Modifier = { Modifier },
    ) {
        this.briefItemContent = itemContent
        this.onItemClicked = onItemClicked
        this.onFocusedItemChanged = onFocusedItemChanged
        this.firstItemModifier = firstItemModifier
        requestedBriefItemIndex.intValue = initialItemIndex
    }

    fun updateBriefItems(items: List<ItemBrief>, focusedIndex: Int) {
        briefItems.value = items
        requestedBriefItemIndex.intValue = focusedIndex
    }

    private fun handleFocusedItemChanged(index: Int) {
        // The pager is removed when the panel auto-hides. Keep its next initial page in sync
        // with the card the user actually reached before that happens.
        requestedBriefItemIndex.intValue = index
        onFocusedItemChanged(index)
    }

    fun updateDisplayConfig(newConfig: DisplayConfig) {
        orientation = newConfig.orientation
        displayConfig = newConfig
    }

    /** Reveal the panel and restart its idle countdown after an interaction. */
    fun showOrResetPanelTimer() {
        if (isGestureRecording.value) return
        isPanelVisible.value = true
        panelTimerTrigger.intValue++
    }

    fun hidePanel() {
        panelTimerTrigger.intValue = 0
        isPointerActive.value = false
        isCarouselScrolling.value = false
        isPanelVisible.value = false
    }

    fun setGestureRecording(recording: Boolean) {
        isGestureRecording.value = recording
        if (recording) hidePanel() else showOrResetPanelTimer()
    }

    fun setPanelAutoHideEnabled(enabled: Boolean) {
        if (isPanelAutoHideEnabled.value == enabled) return
        isPanelAutoHideEnabled.value = enabled
        if (!enabled) {
            panelTimerTrigger.intValue = 0
        } else if (isPanelVisible.value) {
            showOrResetPanelTimer()
        }
    }

    private fun onPointerDown() {
        if (isGestureRecording.value) return
        isPointerActive.value = true
        showOrResetPanelTimer()
    }

    private fun onPointerUp() {
        if (!isPointerActive.value) return
        isPointerActive.value = false
        showOrResetPanelTimer()
    }

    private fun onCarouselScrollChanged(scrolling: Boolean) {
        if (isCarouselScrolling.value == scrolling) return
        isCarouselScrolling.value = scrolling
        if (!scrolling && !isPointerActive.value) showOrResetPanelTimer()
    }

    fun showOrResetInstructionsTimer() {
        isInstructionsVisible.value = true
        instructionsTimerTrigger.intValue++
    }

    fun hideInstructions() {
        instructionsTimerTrigger.intValue = 0
        isInstructionsVisible.value = false
    }

    fun dispose() {
        panelTimerTrigger.intValue = 0
        instructionsTimerTrigger.intValue = 0
    }

    @Composable
    private fun OverlayContent() {
        val context = LocalContext.current
        LaunchedEffect(
            panelTimerTrigger.intValue,
            isPanelAutoHideEnabled.value,
            isPointerActive.value,
            isCarouselScrolling.value,
        ) {
            if (panelTimerTrigger.intValue > 0 && isPanelAutoHideEnabled.value &&
                !isPointerActive.value && !isCarouselScrolling.value
            ) {
                delay(context.recommendedPanelHideDelayMs())
                isPanelVisible.value = false
            }
        }
        LaunchedEffect(instructionsTimerTrigger.intValue) {
            if (instructionsTimerTrigger.intValue > 0) {
                delay(INSTRUCTIONS_HIDE_DELAY_MS)
                isInstructionsVisible.value = false
            }
        }
        val isPortrait = orientation == Configuration.ORIENTATION_PORTRAIT
        Box(
            Modifier.fillMaxSize().pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    val tracksPanel = !isGestureRecording.value
                    if (tracksPanel) onPointerDown()
                    try {
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Final)
                        } while (event.changes.any { it.pressed })
                    } finally {
                        if (tracksPanel) onPointerUp()
                    }
                }
            },
        ) {
            ItemBriefCanvas(
                description = currentDescription.value,
                displayConfig = displayConfig,
                animate = isAnimateEnabled.value,
                modifier = Modifier.fillMaxSize(),
            )
            GestureRecordOverlay(
                displayConfig = displayConfig,
                isRecording = isGestureRecording.value,
                onGestureCaptured = { gesture, isFinished ->
                    internalGestureCaptureListener?.invoke(gesture, isFinished)
                },
                modifier = Modifier.fillMaxSize(),
            )

            // An exiting panel still owns pointer input during its animation. Recording must
            // expose the entire gesture surface immediately, not just hide the panel visually.
            if (!isGestureRecording.value) AnimatedVisibility(
                visible = isPanelVisible.value,
                enter = if (isPortrait) slideInVertically { it } + fadeIn() else slideInHorizontally { -it } + fadeIn(),
                exit = if (isPortrait) {
                    slideOutVertically(
                        animationSpec = tween(PANEL_EXIT_DURATION_MS, easing = FastOutSlowInEasing),
                        targetOffsetY = { (it * 0.12f).roundToInt() },
                    ) + fadeOut(animationSpec = tween(PANEL_EXIT_DURATION_MS, easing = LinearEasing))
                } else {
                    slideOutHorizontally(
                        animationSpec = tween(PANEL_EXIT_DURATION_MS, easing = FastOutSlowInEasing),
                        targetOffsetX = { -(it * 0.12f).roundToInt() },
                    ) + fadeOut(animationSpec = tween(PANEL_EXIT_DURATION_MS, easing = LinearEasing))
                },
            ) {
                if (isPortrait) PortraitBriefPanel() else LandscapeBriefPanel()
            }

            // The legacy root consumed a tap while its auto-hidden panel was away and used it to
            // reveal that panel. Keep that explicit here, but never place a hit target over a
            // visible brief card.
            if (!isPanelVisible.value && !isGestureRecording.value) {
                val interactionSource = remember { MutableInteractionSource() }
                Box(
                    Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = ::showOrResetPanelTimer,
                        ),
                )
            }

            AnimatedVisibility(
                visible = isInstructionsVisible.value,
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Black,
                                0.7f to Color.Black.copy(alpha = 0.53f),
                                1f to Color.Transparent,
                            ),
                        )
                        .padding(start = 32.dp, top = if (isPortrait) 12.dp else 8.dp, end = 32.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val blinkingTransition = rememberInfiniteTransition(label = "BlinkingRecordingIcon")
                    val blinkingAlpha by blinkingTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 0.5f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(500),
                            repeatMode = RepeatMode.Reverse,
                        ),
                        label = "RecordingAlpha",
                    )
                    Icon(
                        painter = painterResource(UiR.drawable.ic_recording),
                        contentDescription = null,
                        tint = colorResource(UiR.color.overlayGestureRecorder),
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer { alpha = blinkingAlpha },
                    )
                    Spacer(Modifier.width(if (isPortrait) 8.dp else 16.dp))
                    Text(
                        text = stringResource(R.string.overlay_instructions_gesture_record),
                        color = colorResource(UiR.color.overlayViewPrimary),
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }

    @Composable
    private fun PortraitBriefPanel() {
        Box(Modifier.fillMaxSize()) {
            PanelTimerResetSurface()
            // The old ConstraintLayout began this surface at the controls minus 112dp. Its
            // effective height is the 68dp controls row plus that offset, not the full screen.
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(PORTRAIT_FADE_HEIGHT)
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.3f to Color.Black.copy(alpha = 0.53f),
                            1f to Color.Black,
                        ),
                    ),
            )
            Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(112.dp))
            Box(Modifier.weight(1f).fillMaxWidth().padding(bottom = 24.dp)) {
                val itemContent = briefItemContent
                if (itemContent != null) {
                    BriefItemsCarousel(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(80.dp),
                        items = briefItems.value,
                        orientation = orientation,
                        requestedIndex = requestedBriefItemIndex.intValue,
                        emptyTextRes = emptyText.intValue,
                        itemContent = itemContent,
                        onItemClicked = onItemClicked,
                        onFocusedItemChanged = ::handleFocusedItemChanged,
                        firstItemModifier = firstItemModifier(),
                        onScrollInProgressChanged = ::onCarouselScrollChanged,
                        onDeleteAnimationChanged = { isDeleteAnimating.value = it },
                    )
                } else if (briefItems.value.isEmpty()) {
                    EmptyBriefCard(
                        emptyTextRes = emptyText.intValue,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(80.dp)
                            .padding(horizontal = 32.dp),
                    )
                }
            }
            Controls()
            }
        }
    }

    @Composable
    private fun LandscapeBriefPanel() {
        Box(Modifier.fillMaxSize()) {
            PanelTimerResetSurface()
            // Legacy used a fade whose right edge ended 156dp beyond the controls.
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(LANDSCAPE_FADE_WIDTH)
                    .background(
                        Brush.horizontalGradient(
                            0f to Color.Black,
                            0.7f to Color.Black.copy(alpha = 0.53f),
                            1f to Color.Transparent,
                        ),
                    ),
            )
            Row(Modifier.fillMaxSize()) {
            Controls()
            Box(Modifier.weight(1f).fillMaxHeight().padding(start = 16.dp, end = 156.dp)) {
                val itemContent = briefItemContent
                if (itemContent != null) {
                    BriefItemsCarousel(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxHeight()
                            .width(124.dp),
                        items = briefItems.value,
                        orientation = orientation,
                        requestedIndex = requestedBriefItemIndex.intValue,
                        emptyTextRes = emptyText.intValue,
                        itemContent = itemContent,
                        onItemClicked = onItemClicked,
                        onFocusedItemChanged = ::handleFocusedItemChanged,
                        firstItemModifier = firstItemModifier(),
                        onScrollInProgressChanged = ::onCarouselScrollChanged,
                        onDeleteAnimationChanged = { isDeleteAnimating.value = it },
                    )
                } else if (briefItems.value.isEmpty()) {
                    EmptyBriefCard(
                        emptyTextRes = emptyText.intValue,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxHeight()
                            .width(124.dp)
                            .padding(vertical = 64.dp),
                    )
                }
            }
            }
        }
    }

    private val isDeleteAnimating = mutableStateOf(false)

    @Composable
    private fun Controls() = ItemBriefControls(
        state = controlState.value,
        isDeleteAnimating = isDeleteAnimating.value,
        isPortrait = orientation == Configuration.ORIENTATION_PORTRAIT,
        onDelete = onDelete,
        onReorder = onReorder,
        onPosition = onPosition,
        onPlay = onPlay,
    )

    /**
     * Equivalent of the legacy root click listener. This sits behind the actual controls and
     * carousel, so only otherwise-unused panel space resets the auto-hide timer.
     */
    @Composable
    private fun PanelTimerResetSurface() {
        val interactionSource = remember { MutableInteractionSource() }
        Box(
            Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = ::showOrResetPanelTimer,
                ),
        )
    }

}

@Composable
private fun EmptyBriefCard(emptyTextRes: Int, modifier: Modifier) {
    ElevatedCard(modifier) {
        Box(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
            if (emptyTextRes != 0) Text(
                text = stringResource(emptyTextRes),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Immutable
data class ItemBriefControlsState(
    val indexText: String = "",
    val canDelete: Boolean = false,
    val canReorder: Boolean = false,
    val canSelectPosition: Boolean = false,
    val canPlay: Boolean = false,
)

@androidx.compose.runtime.Composable
private fun ItemBriefControls(
    state: ItemBriefControlsState,
    isDeleteAnimating: Boolean,
    isPortrait: Boolean,
    onDelete: () -> Unit,
    onReorder: () -> Unit,
    onPosition: () -> Unit,
    onPlay: () -> Unit,
) {
    val canDelete = state.canDelete && !isDeleteAnimating
    val canReorder = state.canReorder && !isDeleteAnimating
    val canSelectPosition = state.canSelectPosition && !isDeleteAnimating
    val canPlay = state.canPlay && !isDeleteAnimating
    if (isPortrait) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 32.dp, end = 32.dp, top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BriefIconButton(UiR.drawable.ic_delete, canDelete, onDelete)
            Spacer(Modifier.width(16.dp))
            BriefIconButton(UiR.drawable.ic_swap_vert, canReorder, onReorder)
            Spacer(Modifier.width(20.dp))
            PositionCard(state, enabled = canSelectPosition, onClick = onPosition, modifier = Modifier.weight(1f).height(48.dp))
            Spacer(Modifier.width(20.dp))
            BriefIconButton(UiR.drawable.ic_play_arrow, canPlay, onPlay)
        }
    } else {
        Column(
            modifier = Modifier.fillMaxHeight().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            BriefIconButton(UiR.drawable.ic_delete, canDelete, onDelete)
            Spacer(Modifier.height(16.dp))
            BriefIconButton(UiR.drawable.ic_swap_vert, canReorder, onReorder)
            Spacer(Modifier.height(20.dp))
            PositionCard(state, enabled = canSelectPosition, onClick = onPosition, modifier = Modifier.width(48.dp))
            Spacer(Modifier.height(20.dp))
            BriefIconButton(UiR.drawable.ic_play_arrow, canPlay, onPlay)
        }
    }
}

@androidx.compose.runtime.Composable
private fun BriefIconButton(icon: Int, enabled: Boolean, onClick: () -> Unit) {
    FilledTonalIconButton(onClick = onClick, enabled = enabled) {
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(24.dp))
    }
}

@androidx.compose.runtime.Composable
private fun PositionCard(
    state: ItemBriefControlsState,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    ElevatedCard(onClick = onClick, enabled = enabled, modifier = modifier) {
        val isMultiline = state.indexText.contains('\n')
        Box(
            modifier = if (isMultiline) {
                Modifier.fillMaxWidth().padding(vertical = 8.dp)
            } else {
                Modifier.fillMaxSize()
            },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = state.indexText,
                color = MaterialTheme.colorScheme.onSurface.copy(
                    alpha = if (enabled) 1f else 0.38f,
                ),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = if (isMultiline) 3 else 1,
            )
        }
    }
}

@Composable
private fun BriefItemsCarousel(
    modifier: Modifier,
    items: List<ItemBrief>,
    orientation: Int,
    requestedIndex: Int,
    emptyTextRes: Int,
    itemContent: @Composable (ItemBrief, Int, () -> Unit) -> Unit,
    onItemClicked: (Int, ItemBrief) -> Unit,
    onFocusedItemChanged: (Int) -> Unit,
    firstItemModifier: Modifier,
    onScrollInProgressChanged: (Boolean) -> Unit,
    onDeleteAnimationChanged: (Boolean) -> Unit,
) {
    var displayedItems by remember { mutableStateOf(items) }
    var deletingItemId by remember { mutableStateOf<Identifier?>(null) }

    val initialPage = remember(displayedItems.isNotEmpty()) {
        if (displayedItems.isEmpty()) 0 else requestedIndex.coerceIn(0, displayedItems.lastIndex)
    }
    val pagerState = rememberPagerState(initialPage = initialPage) { displayedItems.size }
    val context = LocalContext.current
    val maximumFlingVelocity = remember(context) {
        ViewConfiguration.get(context).scaledMaximumFlingVelocity.toFloat()
    }
    val pagerSnapDistance = remember(maximumFlingVelocity) {
        object : PagerSnapDistance {
            override fun calculateTargetPage(
                startPage: Int,
                suggestedTargetPage: Int,
                velocity: Float,
                pageSize: Int,
                pageSpacing: Int,
            ): Int {
                if (maximumFlingVelocity == 0f) return suggestedTargetPage
                val normalizedVelocity = kotlin.math.abs(velocity) / maximumFlingVelocity
                val maxAdditionalPages = (FLING_LINEAR_FACTOR * normalizedVelocity +
                    FLING_QUADRATIC_FACTOR * normalizedVelocity * normalizedVelocity).roundToInt().coerceAtLeast(1)
                return suggestedTargetPage.coerceIn(startPage - maxAdditionalPages, startPage + maxAdditionalPages)
            }
        }
    }
    val flingBehavior = PagerDefaults.flingBehavior(
        state = pagerState,
        pagerSnapDistance = pagerSnapDistance,
        snapAnimationSpec = spring(
            dampingRatio = EXPRESSIVE_SNAP_DAMPING_RATIO,
            stiffness = EXPRESSIVE_SNAP_STIFFNESS,
        ),
    )

    LaunchedEffect(items) {
        if (items == displayedItems) return@LaunchedEffect

        val deletedItem = if (items.size < displayedItems.size) {
            displayedItems.firstOrNull { old -> items.none { it.id == old.id } }
        } else null

        val deletedIndex = if (deletedItem != null) {
            displayedItems.indexOfFirst { it.id == deletedItem.id }
        } else -1

        val shouldAnimate = deletedItem != null && (deletedIndex == pagerState.currentPage || displayedItems.size == 1)

        if (shouldAnimate && deletedItem != null) {
            deletingItemId = deletedItem.id
            onDeleteAnimationChanged(true)
            try {
                // Phase 1: current card vanishes in place in the center
                delay(180)

                // Phase 2: neighboring card glides into center to occupy the position
                if (displayedItems.size > 1) {
                    val targetPage = if (deletedIndex < displayedItems.lastIndex) deletedIndex + 1 else deletedIndex - 1
                    pagerState.animateScrollToPage(
                        page = targetPage,
                        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                    )
                    val newIndex = if (deletedIndex < items.size) deletedIndex else (items.size - 1).coerceAtLeast(0)
                    displayedItems = items
                    deletingItemId = null
                    pagerState.scrollToPage(newIndex)
                } else {
                    displayedItems = items
                    deletingItemId = null
                }
            } finally {
                displayedItems = items
                deletingItemId = null
                onDeleteAnimationChanged(false)
            }
        } else {
            displayedItems = items
            deletingItemId = null
            onDeleteAnimationChanged(false)
        }
    }

    LaunchedEffect(requestedIndex, deletingItemId) {
        if (deletingItemId == null && displayedItems.isNotEmpty() && pagerState.currentPage != requestedIndex) {
            val target = requestedIndex.coerceIn(0, displayedItems.lastIndex)
            pagerState.animateScrollToPage(target)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .collect { page -> onFocusedItemChanged(page) }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.isScrollInProgress }
            .collect(onScrollInProgressChanged)
    }

    if (displayedItems.isEmpty()) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(200)),
            modifier = modifier,
        ) {
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                EmptyBriefCard(
                    emptyTextRes = emptyTextRes,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .padding(horizontal = 32.dp),
                )
            } else {
                EmptyBriefCard(
                    emptyTextRes = emptyTextRes,
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(124.dp)
                        .padding(vertical = 64.dp),
                )
            }
        }
        return
    }

    BoxWithConstraints(modifier = modifier) {
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            val cardFraction = 0.84f
            val horizontalPadding = (maxWidth * (1f - cardFraction)) / 2
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = horizontalPadding),
                pageSpacing = 8.dp,
                flingBehavior = flingBehavior,
                userScrollEnabled = deletingItemId == null,
                key = { page -> displayedItems.getOrNull(page)?.id?.toBundleKey() ?: page },
            ) { page ->
                val brief = displayedItems[page]
                val isDeleting = brief.id == deletingItemId
                BriefItemContainer(
                    modifier = Modifier.fillMaxSize(),
                    firstItemModifier = firstItemModifier,
                    isFirstItem = page == 0,
                    isDeleting = isDeleting,
                    orientation = orientation,
                ) {
                    itemContent(brief, orientation) { if (!isDeleting) onItemClicked(page, brief) }
                }
            }
        } else {
            val cardFraction = 0.72f
            val verticalPadding = (maxHeight * (1f - cardFraction)) / 2
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = verticalPadding),
                pageSpacing = 8.dp,
                flingBehavior = flingBehavior,
                userScrollEnabled = deletingItemId == null,
                key = { page -> displayedItems.getOrNull(page)?.id?.toBundleKey() ?: page },
            ) { page ->
                val brief = displayedItems[page]
                val isDeleting = brief.id == deletingItemId
                BriefItemContainer(
                    modifier = Modifier.fillMaxSize(),
                    firstItemModifier = firstItemModifier,
                    isFirstItem = page == 0,
                    isDeleting = isDeleting,
                    orientation = orientation,
                ) {
                    itemContent(brief, orientation) { if (!isDeleting) onItemClicked(page, brief) }
                }
            }
        }
    }
}

@Composable
private fun BriefItemContainer(
    modifier: Modifier,
    firstItemModifier: Modifier,
    isFirstItem: Boolean,
    isDeleting: Boolean,
    orientation: Int,
    content: @Composable () -> Unit,
) {
    val deleteProgress by animateFloatAsState(
        targetValue = if (isDeleting) 1f else 0f,
        animationSpec = tween(durationMillis = 180, easing = FastOutLinearInEasing),
        label = "deleteProgress",
    )

    Box(
        modifier
            .then(if (isFirstItem) firstItemModifier else Modifier)
            .graphicsLayer {
                val progress = deleteProgress
                alpha = 1f - progress
                scaleX = 1f - 0.25f * progress
                scaleY = 1f - 0.25f * progress
                if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                    translationY = 28.dp.toPx() * progress
                } else {
                    translationX = -28.dp.toPx() * progress
                }
            },
    ) {
        content()
    }
}

private val PORTRAIT_FADE_HEIGHT = 180.dp
private val LANDSCAPE_FADE_WIDTH = 252.dp

private const val FLING_LINEAR_FACTOR = 2f
private const val FLING_QUADRATIC_FACTOR = 2f
private const val EXPRESSIVE_SNAP_DAMPING_RATIO = 0.8f
private const val EXPRESSIVE_SNAP_STIFFNESS = 380f
private const val AUTO_HIDE_DELAY_MS = 3_000L

private fun Context.recommendedPanelHideDelayMs(): Long {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return AUTO_HIDE_DELAY_MS
    val accessibilityManager = getSystemService(AccessibilityManager::class.java) ?: return AUTO_HIDE_DELAY_MS
    return accessibilityManager.getRecommendedTimeoutMillis(
        AUTO_HIDE_DELAY_MS.toInt(),
        AccessibilityManager.FLAG_CONTENT_CONTROLS or
            AccessibilityManager.FLAG_CONTENT_ICONS or
            AccessibilityManager.FLAG_CONTENT_TEXT,
    ).toLong()
}

private fun Identifier.toBundleKey(): String =
    if (tempId != null) "temp_$tempId" else "db_$databaseId"
