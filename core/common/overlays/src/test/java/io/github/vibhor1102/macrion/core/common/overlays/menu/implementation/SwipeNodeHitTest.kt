package io.github.vibhor1102.macrion.core.common.overlays.menu.implementation

import io.github.vibhor1102.macrion.core.base.gesture.SwipePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SwipeNodeHitTest {
    @Test fun closestNodeWinsWhenHitAreasOverlap() {
        val points = listOf(SwipePoint(100f, 100f), SwipePoint(104f, 100f))

        assertEquals(0, findSwipeNodeAt(points, SwipePoint(101f, 100f), 32f))
        assertEquals(1, findSwipeNodeAt(points, SwipePoint(103f, 100f), 32f))
    }

    @Test fun laterNodeSitsOnTopForExactOrEquidistantOverlap() {
        val points = listOf(
            SwipePoint(100f, 100f), SwipePoint(100f, 100f), SwipePoint(102f, 100f),
        )

        assertEquals(1, findSwipeNodeAt(points, SwipePoint(100f, 100f), 32f))
        assertEquals(2, findSwipeNodeAt(points, SwipePoint(101f, 100f), 32f))
        assertNull(findSwipeNodeAt(points, SwipePoint(200f, 100f), 32f))
    }
}
