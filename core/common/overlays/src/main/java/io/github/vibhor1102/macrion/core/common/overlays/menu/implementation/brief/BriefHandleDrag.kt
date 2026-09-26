package io.github.vibhor1102.macrion.core.common.overlays.menu.implementation.brief

import android.graphics.PointF
import io.github.vibhor1102.macrion.core.base.gesture.SwipePoint
import io.github.vibhor1102.macrion.core.base.identifier.Identifier
import io.github.vibhor1102.macrion.core.ui.views.itembrief.ItemBriefDescription
import io.github.vibhor1102.macrion.core.ui.views.itembrief.renderers.ActionCarouselDescription
import io.github.vibhor1102.macrion.core.ui.views.itembrief.renderers.ClickDescription
import io.github.vibhor1102.macrion.core.ui.views.itembrief.renderers.SplitDescription
import io.github.vibhor1102.macrion.core.ui.views.itembrief.renderers.SwipeDescription

enum class BriefHandleKind { CLICK, SWIPE_START, SWIPE_NODE, SWIPE_END }

data class BriefHandle(
    val kind: BriefHandleKind,
    val previewChildIndex: Int?,
    val sourceChildIndex: Int?,
    val nodeIndex: Int?,
    val position: PointF,
)

data class BriefHandleMove(
    val actionId: Identifier,
    val handle: BriefHandle,
    val newPosition: PointF,
)

/** Only the focused card contributes hit targets, even when other cards remain visible. */
internal fun ItemBriefDescription.findFocusedHandle(touch: PointF, hitRadiusPx: Float): BriefHandle? {
    val focused = when (this) {
        is ActionCarouselDescription -> previews.firstOrNull { it.order == focusedOrder }?.description
        else -> this
    } ?: return null
    val handles = buildList {
        fun addAction(action: ItemBriefDescription, previewChildIndex: Int?, sourceChildIndex: Int?) {
            when (action) {
                is ClickDescription -> if (action.imageConditionBitmap == null) action.position?.let {
                    add(BriefHandle(BriefHandleKind.CLICK, previewChildIndex, sourceChildIndex, null, it))
                }
                is SwipeDescription -> if (action.from != null && action.to != null) {
                    val points = action.path?.nodes?.map { PointF(it.position.x, it.position.y) }
                        ?: listOfNotNull(action.from, action.to)
                    points.forEachIndexed { index, point ->
                        val kind = when (index) {
                            0 -> BriefHandleKind.SWIPE_START
                            points.lastIndex -> BriefHandleKind.SWIPE_END
                            else -> BriefHandleKind.SWIPE_NODE
                        }
                        add(BriefHandle(kind, previewChildIndex, sourceChildIndex, index, point))
                    }
                }
                else -> Unit
            }
        }
        if (focused is SplitDescription) {
            focused.subDescriptions.forEachIndexed { index, child ->
                addAction(child, index, focused.sourceIndices.getOrElse(index) { index })
            }
        } else addAction(focused, null, null)
    }
    // A later child or path node is drawn above an earlier one at the same point.
    return handles.withIndex()
        .filter { (_, handle) -> SwipePoint(handle.position).distanceTo(SwipePoint(touch)) <= hitRadiusPx }
        .minWithOrNull(compareBy<IndexedValue<BriefHandle>> {
            SwipePoint(it.value.position).distanceTo(SwipePoint(touch))
        }.thenByDescending { it.index })
        ?.value
}

internal fun ItemBriefDescription.withFocusedHandleMoved(handle: BriefHandle, position: PointF): ItemBriefDescription {
    fun move(action: ItemBriefDescription): ItemBriefDescription {
        return when (action) {
            is ClickDescription -> action.copy(position = position)
            is SwipeDescription -> {
                val nodeIndex = handle.nodeIndex ?: return action
                val movedPath = action.path?.moveNode(nodeIndex, SwipePoint(position))
                val lastIndex = action.path?.nodes?.lastIndex ?: 1
                action.copy(
                    from = if (nodeIndex == 0) position else action.from,
                    to = if (nodeIndex == lastIndex) position else action.to,
                    path = movedPath,
                )
            }
            else -> action
        }
    }
    fun moveChild(action: ItemBriefDescription): ItemBriefDescription {
        if (action !is SplitDescription) return move(action)
        val index = handle.previewChildIndex ?: return action
        if (index !in action.subDescriptions.indices) return action
        return action.copy(subDescriptions = action.subDescriptions.mapIndexed { childIndex, child ->
            if (childIndex == index) move(child) else child
        })
    }
    return when (this) {
        is ActionCarouselDescription -> copy(previews = previews.map { preview ->
            if (preview.order == focusedOrder) preview.copy(description = moveChild(preview.description)) else preview
        })
        else -> moveChild(this)
    }
}

internal fun ItemBriefDescription.focusedHandlePosition(handle: BriefHandle): PointF? {
    val focused = when (this) {
        is ActionCarouselDescription -> previews.firstOrNull { it.order == focusedOrder }?.description
        else -> this
    }
    val action = if (focused is SplitDescription) focused.subDescriptions.getOrNull(handle.previewChildIndex ?: -1) else focused
    return when (action) {
        is ClickDescription -> action.position
        is SwipeDescription -> action.path?.nodes?.getOrNull(handle.nodeIndex ?: -1)?.position?.let { PointF(it.x, it.y) }
            ?: when (handle.nodeIndex) {
                0 -> action.from
                1 -> action.to
                else -> null
            }
        else -> null
    }
}
