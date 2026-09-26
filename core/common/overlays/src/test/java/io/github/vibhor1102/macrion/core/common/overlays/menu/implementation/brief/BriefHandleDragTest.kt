package io.github.vibhor1102.macrion.core.common.overlays.menu.implementation.brief

import android.graphics.PointF
import io.github.vibhor1102.macrion.core.base.gesture.SwipeNode
import io.github.vibhor1102.macrion.core.base.gesture.SwipePath
import io.github.vibhor1102.macrion.core.base.gesture.SwipePoint
import io.github.vibhor1102.macrion.core.ui.views.itembrief.renderers.ActionCarouselDescription
import io.github.vibhor1102.macrion.core.ui.views.itembrief.renderers.ClickDescription
import io.github.vibhor1102.macrion.core.ui.views.itembrief.renderers.NumberedActionPreview
import io.github.vibhor1102.macrion.core.ui.views.itembrief.renderers.SplitDescription
import io.github.vibhor1102.macrion.core.ui.views.itembrief.renderers.SwipeDescription
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BriefHandleDragTest {
    @Test fun onlyFocusedCardCanBeDragged() {
        val carousel = ActionCarouselDescription(
            previews = listOf(
                NumberedActionPreview(1, ClickDescription(position = PointF(20f, 20f))),
                NumberedActionPreview(2, ClickDescription(position = PointF(120f, 120f))),
            ),
            focusedOrder = 2,
        )

        assertNull(carousel.findFocusedHandle(PointF(20f, 20f), 32f))
        assertEquals(BriefHandleKind.CLICK,
            carousel.findFocusedHandle(PointF(120f, 120f), 32f)?.kind)
    }

    @Test fun previewChildMapsBackToOriginalChildIndex() {
        val carousel = ActionCarouselDescription(
            previews = listOf(NumberedActionPreview(3, SplitDescription(
                subDescriptions = listOf(
                    ClickDescription(position = PointF(10f, 10f)),
                    SwipeDescription(from = PointF(100f, 100f), to = PointF(200f, 200f)),
                ),
                sourceIndices = listOf(0, 2),
            ))),
            focusedOrder = 3,
        )

        val handle = carousel.findFocusedHandle(PointF(200f, 200f), 32f)!!
        assertEquals(1, handle.previewChildIndex)
        assertEquals(2, handle.sourceChildIndex)
        assertEquals(BriefHandleKind.SWIPE_END, handle.kind)
        val moved = carousel.withFocusedHandleMoved(handle, PointF(220f, 230f))
        assertEquals(PointF(220f, 230f), moved.focusedHandlePosition(handle))
    }

    @Test fun laterOverlappingNodeWinsAndOtherCurveNodesStayAnchored() {
        val path = SwipePath(listOf(
            SwipeNode(SwipePoint(40f, 40f)),
            SwipeNode(SwipePoint(90f, 90f)),
            SwipeNode(SwipePoint(90f, 90f)),
        ))
        val swipe = SwipeDescription(
            from = PointF(40f, 40f), to = PointF(90f, 90f), path = path,
        )

        val handle = swipe.findFocusedHandle(PointF(90f, 90f), 32f)!!
        assertEquals(2, handle.nodeIndex)
        val moved = swipe.withFocusedHandleMoved(handle, PointF(110f, 110f)) as SwipeDescription
        assertEquals(SwipePoint(90f, 90f), moved.path?.nodes?.get(1)?.position)
        assertEquals(SwipePoint(110f, 110f), moved.path?.end)
    }
}
