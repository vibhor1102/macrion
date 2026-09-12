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

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View

import androidx.annotation.CallSuper
import androidx.annotation.StringRes
import androidx.annotation.StyleRes

import io.github.vibhor1102.macrion.core.common.overlays.R
import io.github.vibhor1102.macrion.core.common.overlays.menu.OverlayMenu
import io.github.vibhor1102.macrion.core.ui.views.itembrief.ItemBriefDescription
import io.github.vibhor1102.macrion.core.ui.views.gesturerecord.toActionDescription

abstract class ItemBriefMenu(
    @StyleRes theme: Int? = null,
    @field:StringRes private val noItemText: Int,
    private val initialItemIndex: Int = 0,
) : OverlayMenu(theme = theme, recreateOverlayViewOnRotation = true) {


    /** The view binding for the position selector. */
    protected lateinit var briefViewBinding: ItemsBriefOverlayViewBinding
    /** Items currently displayed by the Compose carousel. */
    private var briefItems: List<ItemBrief> = emptyList()
    private var focusedItemIndex: Int = 0

    protected open fun onOverlayViewCreated(binding: ItemsBriefOverlayViewBinding): Unit = Unit

    @androidx.compose.runtime.Composable
    protected abstract fun ItemBriefContent(item: ItemBrief, orientation: Int, onClick: () -> Unit)
    protected open fun onFirstBriefItemViewChanged(itemView: View?): Unit = Unit

    protected open fun onItemBriefClicked(index: Int, item: ItemBrief): Unit = Unit
    protected open fun onItemPositionCardClicked(index: Int, itemCount: Int): Unit = Unit
    protected open fun onMoveItemClicked(from: Int, to: Int) = Unit
    protected abstract fun onPlayItemClicked(index: Int)
    protected abstract fun onDeleteItemClicked(index: Int)

    override fun onCreateOverlayView(): View {
        briefViewBinding = ItemsBriefOverlayViewBinding.inflate(
            inflater = context.getSystemService(LayoutInflater::class.java),
            orientation = displayConfigManager.displayConfig.orientation,
            displayConfig = displayConfigManager.displayConfig,
        )

        briefViewBinding.apply {

            setBriefItemsContent(
                initialItemIndex = initialItemIndex,
                itemContent = { item, orientation, onClick -> ItemBriefContent(item, orientation, onClick) },
                onItemClicked = { index, item ->
                    debounceUserInteraction { onItemBriefClicked(index, item) }
                },
                onFocusedItemChanged = { index ->
                    focusedItemIndex = index
                    onFocusedItemChanged(index)
                    showOrResetPanelTimer()
                },
                onFirstItemViewChanged = ::onFirstBriefItemViewChanged,
            )

            setEmptyText(noItemText)

            setControlCallbacks(
                onMovePrevious = { debounceUserInteraction {
                    showOrResetPanelTimer()
                    onMoveItemClicked(focusedItemIndex, focusedItemIndex - 1)
                } },
                onDelete = { debounceUserInteraction {
                    showOrResetPanelTimer()
                    onDeleteItemClicked(focusedItemIndex)
                } },
                onPosition = { debounceUserInteraction {
                    onItemPositionCardClicked(getFocusedItemIndex(), briefItems.size)
                } },
                onPlay = { debounceUserInteraction {
                    onPlayItemClicked(focusedItemIndex)
                } },
                onMoveNext = { debounceUserInteraction {
                    showOrResetPanelTimer()
                    onMoveItemClicked(focusedItemIndex, focusedItemIndex + 1)
                } },
            )
        }

        onFocusedItemChanged(0)
        onOverlayViewCreated(briefViewBinding)
        return briefViewBinding.root
    }

    override fun onResume() {
        super.onResume()
        briefViewBinding.showOrResetPanelTimer()
    }

    override fun onDestroy() {
        briefViewBinding.dispose()
        super.onDestroy()
    }

    override fun onScreenOverlayVisibilityChanged(isVisible: Boolean) {
        if (isVisible) briefViewBinding.showOrResetPanelTimer()
    }

    @CallSuper
    protected open fun onFocusedItemChanged(index: Int) {
        updateBriefButtons(briefItems.size)
    }

    protected fun setBriefPanelAutoHide(isEnabled: Boolean) {
        briefViewBinding.setPanelAutoHideEnabled(isEnabled)
    }

    protected fun getFocusedItemIndex(): Int =
        focusedItemIndex

    protected fun getFocusedItemBrief(): ItemBrief? {
        if (briefItems.isEmpty()) return null
        return briefItems.getOrNull(getFocusedItemIndex().coerceIn(0, briefItems.lastIndex))
    }

    protected fun hidePanel(): Unit =
        briefViewBinding.hidePanel()

    protected fun updateItemList(actions: List<ItemBrief>) {
        val previousItems = briefItems
        val previouslyFocusedId = previousItems.getOrNull(focusedItemIndex)?.id
        briefItems = actions

        val targetIndex = when {
            actions.isEmpty() -> 0
            previousItems.isEmpty() -> initialItemIndex.coerceIn(0, actions.lastIndex)
            actions.size > previousItems.size -> actions.indexOfLast { item -> previousItems.none { it.id == item.id } }
                .takeIf { it >= 0 } ?: focusedItemIndex.coerceIn(0, actions.lastIndex)
            else -> actions.indexOfFirst { it.id == previouslyFocusedId }
                .takeIf { it >= 0 } ?: focusedItemIndex.coerceIn(0, actions.lastIndex)
        }
        focusedItemIndex = targetIndex
        briefViewBinding.updateBriefItems(actions, targetIndex)

        updateBriefButtons(actions.size)
    }

    @SuppressLint("ClickableViewAccessibility")
    protected fun startGestureCapture(onNewAction: (gesture: ItemBriefDescription?, isFinished: Boolean) -> Unit) {
        briefViewBinding.setGestureRecording(true)
        briefViewBinding.showOrResetInstructionsTimer()
        briefViewBinding.viewBrief.setDescription(null)

        briefViewBinding.viewRecorder.apply {
            isVisible = true

            var isCaptureStarted = false
            gestureCaptureListener = { gesture, isFinished ->
                if (gesture != null && !isCaptureStarted){
                    isCaptureStarted = true
                    briefViewBinding.hideInstructions()
                }
                briefViewBinding.viewBrief.setDescription(
                    newDescription = gesture?.toActionDescription(),
                    animate = isFinished,
                )

                if (isFinished) {
                    stopGestureCapture()
                    onNewAction(gesture?.toActionDescription(), true)
                }
            }
        }
    }

    protected fun stopGestureCapture() {
        briefViewBinding.viewRecorder.clearAndHide()
        briefViewBinding.setGestureRecording(false)
        briefViewBinding.hideInstructions()
    }

    protected fun isGestureCaptureStarted(): Boolean =
        briefViewBinding.viewRecorder.isVisible

    private fun updateBriefButtons(itemCount: Int) {
        val index = focusedItemIndex.coerceIn(0, (itemCount - 1).coerceAtLeast(0))
        val hasItems = itemCount != 0
        briefViewBinding.updateControls(
            ItemBriefControlsState(
                indexText = getIndexText(currentIndex = if (hasItems) index + 1 else 0, itemCount = itemCount),
                canMovePrevious = hasItems && index != 0,
                canDelete = hasItems,
                canSelectPosition = hasItems,
                canPlay = hasItems,
                canMoveNext = hasItems && index != itemCount - 1,
            )
        )
    }

    private fun getIndexText(currentIndex: Int, itemCount: Int): String =
        if (displayConfigManager.displayConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
            context.getString(R.string.item_brief_items_count_land, currentIndex, itemCount)
        else context.getString(R.string.item_brief_items_count_port, currentIndex, itemCount)
}
