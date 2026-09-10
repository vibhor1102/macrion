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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewConfiguration
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Space
import androidx.compose.animation.core.spring
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.snapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.widget.ConstraintLayout
import io.github.vibhor1102.macrion.core.common.overlays.R
import io.github.vibhor1102.macrion.core.ui.views.gesturerecord.GestureRecordView
import io.github.vibhor1102.macrion.core.ui.views.itembrief.ItemBriefView
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.core.ui.R as UiR

class ItemsBriefOverlayViewBinding private constructor(
    val root: View,
    val viewBrief: ItemBriefView,
    val viewRecorder: GestureRecordView,
    val layoutInstructions: ComposeView,
    val layoutActionList: View,
    val listActions: ComposeView,
    val emptyScenarioCard: View,
    private val controlPanel: ComposeView,
    private val orientation: Int,
) {

    val recordingIcon = ImageView(root.context).apply {
        setImageResource(UiR.drawable.ic_recording)
    }
    private val emptyText = mutableIntStateOf(0)
    private val controlState = mutableStateOf(ItemBriefControlsState())
    private val briefItems = mutableStateOf<List<ItemBrief>>(emptyList())
    private val requestedBriefItemIndex = mutableIntStateOf(0)

    private var briefItemContent: (@Composable (ItemBrief, Int, () -> Unit) -> Unit)? = null
    private var onItemClicked: (Int, ItemBrief) -> Unit = { _, _ -> }
    private var onFocusedItemChanged: (Int) -> Unit = {}
    private var onFirstItemViewChanged: (View?) -> Unit = {}

    private var onMovePrevious: () -> Unit = {}
    private var onDelete: () -> Unit = {}
    private var onPosition: () -> Unit = {}
    private var onPlay: () -> Unit = {}
    private var onMoveNext: () -> Unit = {}

    private constructor(views: HostViews, orientation: Int) : this(
        root = views.root,
        viewBrief = views.viewBrief,
        viewRecorder = views.viewRecorder,
        layoutInstructions = views.layoutInstructions,
        layoutActionList = views.layoutActionList,
        listActions = views.listActions,
        emptyScenarioCard = views.emptyScenarioCard,
        controlPanel = views.controlPanel,
        orientation = orientation,
    )

    companion object {

        fun inflate(inflater: LayoutInflater, orientation: Int): ItemsBriefOverlayViewBinding {
            val isPortrait = orientation == Configuration.ORIENTATION_PORTRAIT
            val views = createHostViews(inflater, isPortrait)
            return ItemsBriefOverlayViewBinding(views, orientation).apply {
                views.backgroundList.setFade(if (isPortrait) FadeDirection.BOTTOM else FadeDirection.LEFT)
                views.emptyScenarioCard.setEmptyContent(emptyText)
                setInstructionsContent(isPortrait)
                setControlPanelContent()
            }
        }

        private fun createHostViews(inflater: LayoutInflater, isPortrait: Boolean): HostViews {
            val context = inflater.context
            val resources = context.resources
            val matchParent = ViewGroup.LayoutParams.MATCH_PARENT
            val wrapContent = ViewGroup.LayoutParams.WRAP_CONTENT

            val root = FrameLayout(context).apply {
                layoutParams = FrameLayout.LayoutParams(matchParent, matchParent)
            }
            val viewRecorder = GestureRecordView(context).apply {
                visibility = View.GONE
            }
            val layoutInstructions = ComposeView(context).apply {
                visibility = View.GONE
            }
            val viewBrief = ItemBriefView(context)
            val layoutActionList = ConstraintLayout(context)
            val backgroundList = ComposeView(context).apply { id = View.generateViewId() }
            val emptyScenarioCard = ComposeView(context).apply {
                id = View.generateViewId()
                visibility = View.GONE
            }
            val listActions = ComposeView(context).apply { id = View.generateViewId() }
            val controlPanel = ComposeView(context).apply { id = View.generateViewId() }
            val spacer = Space(context).apply { id = View.generateViewId() }

            root.addView(viewRecorder, FrameLayout.LayoutParams(matchParent, matchParent))
            root.addView(layoutInstructions, FrameLayout.LayoutParams(matchParent, wrapContent))
            root.addView(viewBrief, FrameLayout.LayoutParams(matchParent, matchParent))
            root.addView(layoutActionList, FrameLayout.LayoutParams(matchParent, matchParent))

            if (isPortrait) {
                layoutActionList.addView(
                    backgroundList,
                    ConstraintLayout.LayoutParams(0, 0).apply {
                        startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                        endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                        topToTop = spacer.id
                        bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                    },
                )
                layoutActionList.addView(
                    spacer,
                    ConstraintLayout.LayoutParams(0, 0).apply {
                        startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                        endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                        bottomToTop = controlPanel.id
                        bottomMargin = resources.getDimensionPixelSize(R.dimen.overlay_brief_background_top_padding_port)
                    },
                )
                layoutActionList.addView(
                    emptyScenarioCard,
                    ConstraintLayout.LayoutParams(
                        0,
                        resources.getDimensionPixelSize(R.dimen.item_brief_height),
                    ).apply {
                        startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                        endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                        bottomToTop = controlPanel.id
                        marginStart = resources.getDimensionPixelSize(UiR.dimen.margin_horizontal_extra_large)
                        marginEnd = resources.getDimensionPixelSize(UiR.dimen.margin_horizontal_extra_large)
                        bottomMargin = resources.getDimensionPixelSize(UiR.dimen.margin_vertical_extra_large)
                    },
                )
                layoutActionList.addView(
                    listActions,
                    ConstraintLayout.LayoutParams(0, 0).apply {
                        startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                        endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                        topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                        bottomToTop = controlPanel.id
                        bottomMargin = resources.getDimensionPixelSize(UiR.dimen.margin_vertical_extra_large)
                    },
                )
                layoutActionList.addView(
                    controlPanel,
                    ConstraintLayout.LayoutParams(0, wrapContent).apply {
                        startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                        endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                        bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                    },
                )
            } else {
                layoutActionList.addView(
                    backgroundList,
                    ConstraintLayout.LayoutParams(0, 0).apply {
                        startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                        endToStart = spacer.id
                        topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                        bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                    },
                )
                layoutActionList.addView(
                    spacer,
                    ConstraintLayout.LayoutParams(0, 0).apply {
                        startToEnd = controlPanel.id
                        topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                        bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                        marginStart = resources.getDimensionPixelSize(R.dimen.overlay_brief_background_end_padding_land)
                    },
                )
                layoutActionList.addView(
                    emptyScenarioCard,
                    ConstraintLayout.LayoutParams(
                        resources.getDimensionPixelSize(R.dimen.overlay_brief_item_width_land),
                        0,
                    ).apply {
                        startToEnd = controlPanel.id
                        topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                        bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                        marginStart = resources.getDimensionPixelSize(UiR.dimen.margin_horizontal_default)
                        topMargin = 64.dpToPx(resources.displayMetrics.density)
                        bottomMargin = 64.dpToPx(resources.displayMetrics.density)
                    },
                )
                layoutActionList.addView(
                    listActions,
                    ConstraintLayout.LayoutParams(0, 0).apply {
                        startToEnd = controlPanel.id
                        endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                        topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                        bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                        marginStart = resources.getDimensionPixelSize(UiR.dimen.margin_horizontal_default)
                    },
                )
                layoutActionList.addView(
                    controlPanel,
                    ConstraintLayout.LayoutParams(wrapContent, 0).apply {
                        startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                        topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                        bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                    },
                )
            }

            return HostViews(
                root = root,
                viewBrief = viewBrief,
                viewRecorder = viewRecorder,
                layoutInstructions = layoutInstructions,
                layoutActionList = layoutActionList,
                backgroundList = backgroundList,
                listActions = listActions,
                emptyScenarioCard = emptyScenarioCard,
                controlPanel = controlPanel,
            )
        }
    }

    private data class HostViews(
        val root: FrameLayout,
        val viewBrief: ItemBriefView,
        val viewRecorder: GestureRecordView,
        val layoutInstructions: ComposeView,
        val layoutActionList: ConstraintLayout,
        val backgroundList: ComposeView,
        val listActions: ComposeView,
        val emptyScenarioCard: ComposeView,
        val controlPanel: ComposeView,
    )

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

        listActions.setContent {
            MacrionTheme {
                BriefItemsCarousel(
                    items = briefItems.value,
                    orientation = orientation,
                    requestedIndex = requestedBriefItemIndex.intValue,
                    itemContent = briefItemContent ?: return@MacrionTheme,
                    onItemClicked = this@ItemsBriefOverlayViewBinding.onItemClicked,
                    onFocusedItemChanged = this@ItemsBriefOverlayViewBinding.onFocusedItemChanged,
                    onFirstItemViewChanged = this@ItemsBriefOverlayViewBinding.onFirstItemViewChanged,
                )
            }
        }
    }

    fun updateBriefItems(items: List<ItemBrief>, focusedIndex: Int) {
        briefItems.value = items
        requestedBriefItemIndex.intValue = focusedIndex
        listActions.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
        emptyScenarioCard.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun setControlPanelContent() {
        controlPanel.setContent {
            MacrionTheme {
                ItemBriefControls(
                    state = controlState.value,
                    isPortrait = orientation == Configuration.ORIENTATION_PORTRAIT,
                    onMovePrevious = { onMovePrevious() },
                    onDelete = { onDelete() },
                    onPosition = { onPosition() },
                    onPlay = { onPlay() },
                    onMoveNext = { onMoveNext() },
                )
            }
        }
    }

    private fun setInstructionsContent(isPortrait: Boolean) {
        layoutInstructions.setContent {
            val colors = arrayOf(
                0f to Color.Black,
                0.7f to Color.Black.copy(alpha = 0.53f),
                1f to Color.Transparent,
            )
            MacrionTheme {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(colorStops = colors))
                        .padding(
                            start = 32.dp,
                            top = if (isPortrait) 12.dp else 8.dp,
                            end = 32.dp,
                            bottom = 24.dp,
                        ),
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

private enum class FadeDirection { TOP, BOTTOM, LEFT }

private fun Int.dpToPx(density: Float): Int = (this * density).toInt()

private fun ComposeView.setFade(direction: FadeDirection) {
    isClickable = false
    setContent {
        val opaque = Color.Black
        val middle = Color.Black.copy(alpha = 0.53f)
        val transparent = Color.Transparent
        val colors = when (direction) {
            FadeDirection.TOP -> arrayOf(0f to opaque, 0.7f to middle, 1f to transparent)
            FadeDirection.BOTTOM -> arrayOf(0f to transparent, 0.3f to middle, 1f to opaque)
            FadeDirection.LEFT -> arrayOf(0f to opaque, 0.7f to middle, 1f to transparent)
        }
        val brush = if (direction == FadeDirection.LEFT) {
            Brush.horizontalGradient(colorStops = colors)
        } else {
            Brush.verticalGradient(colorStops = colors)
        }
        Box(Modifier.fillMaxSize().background(brush))
    }
}

private fun ComposeView.setEmptyContent(textState: androidx.compose.runtime.MutableIntState) {
    setContent {
        MacrionTheme {
            ElevatedCard(Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (textState.intValue != 0) {
                        Text(
                            text = stringResource(textState.intValue),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BriefItemsCarousel(
    items: List<ItemBrief>,
    orientation: Int,
    requestedIndex: Int,
    itemContent: @Composable (ItemBrief, Int, () -> Unit) -> Unit,
    onItemClicked: (Int, ItemBrief) -> Unit,
    onFocusedItemChanged: (Int) -> Unit,
    onFirstItemViewChanged: (View?) -> Unit,
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
            modifier = Modifier.fillMaxSize(),
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            itemsIndexed(items, key = { _, brief -> brief.id.toString() }) { index, brief ->
                BriefItemContainer(
                    modifier = Modifier.fillParentMaxSize(),
                    isFirstItem = index == 0,
                    onFirstItemViewChanged = onFirstItemViewChanged,
                    onClick = { onItemClicked(index, brief) },
                ) {
                    itemContent(brief, orientation) { onItemClicked(index, brief) }
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = 64.dp),
        ) {
            itemsIndexed(items, key = { _, brief -> brief.id.toString() }) { index, brief ->
                BriefItemContainer(
                    modifier = Modifier.fillParentMaxSize(),
                    isFirstItem = index == 0,
                    onFirstItemViewChanged = onFirstItemViewChanged,
                    onClick = { onItemClicked(index, brief) },
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
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(modifier) {
        if (isFirstItem) {
            val context = LocalContext.current
            AndroidView(
                factory = {
                    View(context).apply { setOnClickListener { onClick() } }.also(onFirstItemViewChanged)
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
