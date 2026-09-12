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

import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewConfiguration
import android.widget.ImageView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.snapFlingBehavior
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.vibhor1102.macrion.core.common.overlays.R
import io.github.vibhor1102.macrion.core.ui.views.gesturerecord.GestureRecordView
import io.github.vibhor1102.macrion.core.ui.views.itembrief.ItemBriefView
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.core.ui.R as UiR

class ItemsBriefOverlayViewBinding private constructor(
    val root: ComposeView,
    val viewBrief: ItemBriefView,
    val viewRecorder: GestureRecordView,
    private val orientation: Int,
) {

    val recordingIcon = ImageView(root.context).apply {
        setImageResource(UiR.drawable.ic_recording)
    }
    private val emptyText = mutableIntStateOf(0)
    private val controlState = mutableStateOf(ItemBriefControlsState())
    private val briefItems = mutableStateOf<List<ItemBrief>>(emptyList())
    private val requestedBriefItemIndex = mutableIntStateOf(0)
    private val isPanelVisible = mutableStateOf(false)
    private val isGestureRecording = mutableStateOf(false)
    private val isInstructionsVisible = mutableStateOf(false)
    private val isPanelAutoHideEnabled = mutableStateOf(true)

    private val mainHandler = Handler(Looper.getMainLooper())
    private val hidePanelRunnable = Runnable { isPanelVisible.value = false }
    private val hideInstructionsRunnable = Runnable { isInstructionsVisible.value = false }

    private var briefItemContent: (@Composable (ItemBrief, Int, () -> Unit) -> Unit)? = null
    private var onItemClicked: (Int, ItemBrief) -> Unit = { _, _ -> }
    private var onFocusedItemChanged: (Int) -> Unit = {}
    private var onFirstItemViewChanged: (View?) -> Unit = {}

    private var onMovePrevious: () -> Unit = {}
    private var onDelete: () -> Unit = {}
    private var onPosition: () -> Unit = {}
    private var onPlay: () -> Unit = {}
    private var onMoveNext: () -> Unit = {}

    companion object {

        private const val AUTO_HIDE_DELAY_MS = 3_000L

        fun inflate(inflater: LayoutInflater, orientation: Int): ItemsBriefOverlayViewBinding {
            val context = inflater.context
            val viewRecorder = GestureRecordView(context).apply {
                visibility = View.GONE
            }
            val viewBrief = ItemBriefView(context)
            val root = ComposeView(context)
            return ItemsBriefOverlayViewBinding(root, viewBrief, viewRecorder, orientation).apply {
                root.setContent { MacrionTheme { OverlayContent() } }
            }
        }
    }

    fun setEmptyText(textRes: Int) {
        emptyText.intValue = textRes
    }

    fun setControlCallbacks(
        onMovePrevious: () -> Unit,
        onDelete: () -> Unit,
        onPosition: () -> Unit,
        onPlay: () -> Unit,
        onMoveNext: () -> Unit,
    ) {
        this.onMovePrevious = onMovePrevious
        this.onDelete = onDelete
        this.onPosition = onPosition
        this.onPlay = onPlay
        this.onMoveNext = onMoveNext
    }

    fun updateControls(state: ItemBriefControlsState) {
        controlState.value = state
    }

    fun setBriefItemsContent(
        initialItemIndex: Int,
        itemContent: @Composable (ItemBrief, Int, () -> Unit) -> Unit,
        onItemClicked: (Int, ItemBrief) -> Unit,
        onFocusedItemChanged: (Int) -> Unit,
        onFirstItemViewChanged: (View?) -> Unit,
    ) {
        this.briefItemContent = itemContent
        this.onItemClicked = onItemClicked
        this.onFocusedItemChanged = onFocusedItemChanged
        this.onFirstItemViewChanged = onFirstItemViewChanged
        requestedBriefItemIndex.intValue = initialItemIndex

    }

    fun updateBriefItems(items: List<ItemBrief>, focusedIndex: Int) {
        briefItems.value = items
        requestedBriefItemIndex.intValue = focusedIndex
    }

    /** Mirrors the legacy brief panel's immediate reveal and three-second auto-hide timer. */
    fun showOrResetPanelTimer() {
        if (isGestureRecording.value) return
        isPanelVisible.value = true
        mainHandler.removeCallbacks(hidePanelRunnable)
        if (isPanelAutoHideEnabled.value) mainHandler.postDelayed(hidePanelRunnable, AUTO_HIDE_DELAY_MS)
    }

    fun hidePanel() {
        mainHandler.removeCallbacks(hidePanelRunnable)
        isPanelVisible.value = false
    }

    fun setGestureRecording(recording: Boolean) {
        isGestureRecording.value = recording
        if (recording) hidePanel() else showOrResetPanelTimer()
    }

    fun setPanelAutoHideEnabled(enabled: Boolean) {
        isPanelAutoHideEnabled.value = enabled
        if (!enabled) mainHandler.removeCallbacks(hidePanelRunnable)
    }

    fun showOrResetInstructionsTimer() {
        isInstructionsVisible.value = true
        mainHandler.removeCallbacks(hideInstructionsRunnable)
        mainHandler.postDelayed(hideInstructionsRunnable, AUTO_HIDE_DELAY_MS)
    }

    fun hideInstructions() {
        mainHandler.removeCallbacks(hideInstructionsRunnable)
        isInstructionsVisible.value = false
    }

    fun dispose() {
        mainHandler.removeCallbacksAndMessages(null)
    }

    @Composable
    private fun OverlayContent() {
        val isPortrait = orientation == Configuration.ORIENTATION_PORTRAIT
        Box(Modifier.fillMaxSize()) {
            AndroidView(factory = { viewBrief }, modifier = Modifier.fillMaxSize())
            AndroidView(factory = { viewRecorder }, modifier = Modifier.fillMaxSize())

            // An exiting panel still owns pointer input during its animation. Recording must
            // expose the entire gesture surface immediately, not just hide the panel visually.
            if (!isGestureRecording.value) AnimatedVisibility(
                visible = isPanelVisible.value,
                enter = if (isPortrait) slideInVertically { it } + fadeIn() else slideInHorizontally { -it } + fadeIn(),
                exit = if (isPortrait) slideOutVertically { it } + fadeOut() else slideOutHorizontally { -it } + fadeOut(),
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
                    AndroidView(factory = { recordingIcon }, modifier = Modifier.size(24.dp))
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
                if (briefItems.value.isEmpty()) {
                    EmptyBriefCard(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(80.dp)
                            .padding(horizontal = 32.dp),
                    )
                }
                else briefItemContent?.let { itemContent ->
                    BriefItemsCarousel(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(80.dp),
                        items = briefItems.value,
                        orientation = orientation,
                        requestedIndex = requestedBriefItemIndex.intValue,
                        itemContent = itemContent,
                        onItemClicked = onItemClicked,
                        onFocusedItemChanged = onFocusedItemChanged,
                        onFirstItemViewChanged = onFirstItemViewChanged,
                        onInteraction = ::showOrResetPanelTimer,
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
                if (briefItems.value.isEmpty()) {
                    EmptyBriefCard(
                        Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxHeight()
                            .width(124.dp)
                            .padding(vertical = 64.dp),
                    )
                }
                else briefItemContent?.let { itemContent ->
                    BriefItemsCarousel(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxHeight()
                            .width(124.dp),
                        items = briefItems.value,
                        orientation = orientation,
                        requestedIndex = requestedBriefItemIndex.intValue,
                        itemContent = itemContent,
                        onItemClicked = onItemClicked,
                        onFocusedItemChanged = onFocusedItemChanged,
                        onFirstItemViewChanged = onFirstItemViewChanged,
                        onInteraction = ::showOrResetPanelTimer,
                    )
                }
            }
            }
        }
    }

    @Composable
    private fun Controls() = ItemBriefControls(
        state = controlState.value,
        isPortrait = orientation == Configuration.ORIENTATION_PORTRAIT,
        onMovePrevious = onMovePrevious,
        onDelete = onDelete,
        onPosition = onPosition,
        onPlay = onPlay,
        onMoveNext = onMoveNext,
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

    @Composable
    private fun EmptyBriefCard(modifier: Modifier) {
        ElevatedCard(modifier) {
            Box(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                if (emptyText.intValue != 0) Text(
                    text = stringResource(emptyText.intValue),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Immutable
data class ItemBriefControlsState(
    val indexText: String = "",
    val canMovePrevious: Boolean = false,
    val canDelete: Boolean = false,
    val canSelectPosition: Boolean = false,
    val canPlay: Boolean = false,
    val canMoveNext: Boolean = false,
)

@androidx.compose.runtime.Composable
private fun ItemBriefControls(
    state: ItemBriefControlsState,
    isPortrait: Boolean,
    onMovePrevious: () -> Unit,
    onDelete: () -> Unit,
    onPosition: () -> Unit,
    onPlay: () -> Unit,
    onMoveNext: () -> Unit,
) {
    if (isPortrait) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 32.dp, end = 32.dp, top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BriefIconButton(UiR.drawable.ic_move_left, state.canMovePrevious, onMovePrevious)
            Spacer(Modifier.width(16.dp))
            BriefIconButton(UiR.drawable.ic_delete, state.canDelete, onDelete)
            Spacer(Modifier.width(32.dp))
            PositionCard(state, onPosition, Modifier.weight(1f).height(48.dp))
            Spacer(Modifier.width(32.dp))
            BriefIconButton(UiR.drawable.ic_play_arrow, state.canPlay, onPlay)
            Spacer(Modifier.width(16.dp))
            BriefIconButton(UiR.drawable.ic_move_right, state.canMoveNext, onMoveNext)
        }
    } else {
        Column(
            modifier = Modifier.fillMaxHeight().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            BriefIconButton(UiR.drawable.ic_move_up, state.canMovePrevious, onMovePrevious)
            Spacer(Modifier.height(16.dp))
            BriefIconButton(UiR.drawable.ic_delete, state.canDelete, onDelete)
            Spacer(Modifier.height(28.dp))
            PositionCard(state, onPosition, Modifier.width(48.dp))
            Spacer(Modifier.height(28.dp))
            BriefIconButton(UiR.drawable.ic_play_arrow, state.canPlay, onPlay)
            Spacer(Modifier.height(16.dp))
            BriefIconButton(UiR.drawable.ic_move_down, state.canMoveNext, onMoveNext)
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
private fun PositionCard(state: ItemBriefControlsState, onClick: () -> Unit, modifier: Modifier) {
    ElevatedCard(onClick = onClick, enabled = state.canSelectPosition, modifier = modifier) {
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
                    alpha = if (state.canSelectPosition) 1f else 0.38f,
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
    itemContent: @Composable (ItemBrief, Int, () -> Unit) -> Unit,
    onItemClicked: (Int, ItemBrief) -> Unit,
    onFocusedItemChanged: (Int) -> Unit,
    onFirstItemViewChanged: (View?) -> Unit,
    onInteraction: () -> Unit,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(items, requestedIndex) {
        if (items.isEmpty()) {
            onFirstItemViewChanged(null)
        } else {
            listState.scrollToItem(requestedIndex.coerceIn(0, items.lastIndex))
        }
    }
    LaunchedEffect(listState, items.size) {
        snapshotFlow { listState.focusedItemIndex() }
            .collect { index -> if (index != null) onFocusedItemChanged(index) }
    }

    val flingBehavior = rememberBriefCarouselFlingBehavior(listState)
    if (orientation == Configuration.ORIENTATION_PORTRAIT) {
        LazyRow(
            modifier = modifier,
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            itemsIndexed(items, key = { _, brief -> brief.id.toString() }) { index, brief ->
                BriefItemContainer(
                    modifier = Modifier.fillParentMaxSize(),
                    isFirstItem = index == 0,
                    onFirstItemViewChanged = onFirstItemViewChanged,
                    onInteraction = onInteraction,
                ) {
                    itemContent(brief, orientation) { onItemClicked(index, brief) }
                }
            }
        }
    } else {
        LazyColumn(
            modifier = modifier,
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = 64.dp),
        ) {
            itemsIndexed(items, key = { _, brief -> brief.id.toString() }) { index, brief ->
                BriefItemContainer(
                    modifier = Modifier.fillParentMaxSize(),
                    isFirstItem = index == 0,
                    onFirstItemViewChanged = onFirstItemViewChanged,
                    onInteraction = onInteraction,
                ) {
                    itemContent(brief, orientation) { onItemClicked(index, brief) }
                }
            }
        }
    }
}

@Composable
private fun rememberBriefCarouselFlingBehavior(listState: LazyListState): androidx.compose.foundation.gestures.FlingBehavior {
    val context = LocalContext.current
    val decayAnimationSpec = rememberSplineBasedDecay<Float>()
    val maximumFlingVelocity = remember(context) {
        ViewConfiguration.get(context).scaledMaximumFlingVelocity.toFloat()
    }
    val defaultSnapProvider = remember(listState) { SnapLayoutInfoProvider(listState) }
    val tunedSnapProvider = remember(listState, defaultSnapProvider, maximumFlingVelocity) {
        object : SnapLayoutInfoProvider {
            override fun calculateApproachOffset(velocity: Float, decayOffset: Float): Float {
                val defaultApproach = defaultSnapProvider.calculateApproachOffset(velocity, decayOffset)
                val pageSize = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: return defaultApproach
                if (pageSize == 0 || maximumFlingVelocity == 0f) return defaultApproach

                // A gently accelerating quadratic keeps the response continuous and predictable:
                // its slope changes linearly and its second derivative is constant. There are no
                // velocity thresholds or page-count caps.
                val normalizedVelocity = kotlin.math.abs(velocity) / maximumFlingVelocity
                val additionalPages =
                    FLING_LINEAR_FACTOR * normalizedVelocity +
                        FLING_QUADRATIC_FACTOR * normalizedVelocity * normalizedVelocity
                val desiredApproach = additionalPages * pageSize

                return kotlin.math.min(desiredApproach, kotlin.math.abs(defaultApproach)) *
                    kotlin.math.sign(decayOffset)
            }

            override fun calculateSnapOffset(velocity: Float): Float =
                defaultSnapProvider.calculateSnapOffset(velocity)
        }
    }
    return remember(tunedSnapProvider, decayAnimationSpec) {
        snapFlingBehavior(
            snapLayoutInfoProvider = tunedSnapProvider,
            decayAnimationSpec = decayAnimationSpec,
            snapAnimationSpec = spring(
                dampingRatio = EXPRESSIVE_SNAP_DAMPING_RATIO,
                stiffness = EXPRESSIVE_SNAP_STIFFNESS,
            ),
        )
    }
}

@Composable
private fun BriefItemContainer(
    modifier: Modifier,
    isFirstItem: Boolean,
    onFirstItemViewChanged: (View?) -> Unit,
    onInteraction: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier.pointerInput(onInteraction) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                onInteraction()
                waitForUpOrCancellation()
            }
        },
    ) {
        if (isFirstItem) {
            val context = LocalContext.current
            AndroidView(
                factory = {
                    // This native child is a tutorial-monitoring anchor only. Card clicks are
                    // handled by the actual composable ElevatedCard above it.
                    View(context).also(onFirstItemViewChanged)
                },
                modifier = Modifier.matchParentSize(),
            )
        }
        content()
    }
}

private fun LazyListState.focusedItemIndex(): Int? {
    val visibleItems = layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) return null

    val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
    return visibleItems.minBy { item ->
        kotlin.math.abs(item.offset + item.size / 2 - viewportCenter)
    }.index
}

private const val FLING_LINEAR_FACTOR = 2f
private const val FLING_QUADRATIC_FACTOR = 2f
private const val EXPRESSIVE_SNAP_DAMPING_RATIO = 0.8f
private const val EXPRESSIVE_SNAP_STIFFNESS = 380f
private val PORTRAIT_FADE_HEIGHT = 180.dp
private val LANDSCAPE_FADE_WIDTH = 252.dp
