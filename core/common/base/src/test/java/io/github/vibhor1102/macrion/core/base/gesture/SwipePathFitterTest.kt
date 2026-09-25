package io.github.vibhor1102.macrion.core.base.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class SwipePathFitterTest {
    @Test fun straightTraceNeedsOnlyItsEndpoints() {
        val trace = (0..20).map { SwipePoint(it * 10f, 20f) }

        val path = fitSwipePath(trace)!!

        assertEquals(2, path.nodes.size)
        assertFalse(path.isCurved)
        assertTrue(path.isValid())
    }

    @Test fun sharpTurnStaysAnchored() {
        val trace = listOf(
            SwipePoint(0f, 0f), SwipePoint(50f, 0f), SwipePoint(100f, 0f),
            SwipePoint(100f, 50f), SwipePoint(100f, 100f),
        )

        val path = fitSwipePath(trace)!!

        assertTrue(path.nodes.any { it.position == SwipePoint(100f, 0f) })
        assertTrue(path.isCurved)
    }

    @Test fun closedLoopDoesNotCollapseToSinglePoint() {
        val trace = listOf(
            SwipePoint(50f, 0f), SwipePoint(100f, 50f), SwipePoint(50f, 100f),
            SwipePoint(0f, 50f), SwipePoint(50f, 0f),
        )

        val path = fitSwipePath(trace)!!

        assertEquals(path.start, path.end)
        assertTrue(path.nodes.size >= 4)
        assertTrue(path.isValid())
    }

    @Test fun retracingTheSameLinePreservesTheReturnTrip() {
        val trace = listOf(
            SwipePoint(0f, 0f), SwipePoint(50f, 0f), SwipePoint(100f, 0f),
            SwipePoint(50f, 0f), SwipePoint(0f, 0f),
            SwipePoint(50f, 0f), SwipePoint(100f, 0f),
        )

        val path = fitSwipePath(trace)!!

        assertEquals(trace.first(), path.start)
        assertEquals(trace.last(), path.end)
        assertTrue(path.nodes.size >= 4)
        assertTrue(path.nodes.drop(1).any { it.position == SwipePoint(0f, 0f) })
        assertTrue(path.isValid())
    }

    @Test fun smoothArcDoesNotBecomeAHandleAtEverySample() {
        val trace = (0..60).map { index ->
            val angle = PI * index / 120.0
            SwipePoint((100 * cos(angle)).toFloat(), (100 * sin(angle)).toFloat())
        }

        val path = fitSwipePath(trace)!!

        assertTrue(path.nodes.size <= 6)
        assertTrue(path.isValid())
    }

    @Test fun persistedPathAndLocalNodeMoveRetainOtherAnchors() {
        val path = fitSwipePath(listOf(
            SwipePoint(0f, 0f), SwipePoint(40f, 0f), SwipePoint(80f, 40f),
            SwipePoint(80f, 80f),
        ))!!
        val converter = SwipePathRoomConverter()

        assertEquals(path, converter.fromColumn(converter.toColumn(path)))
        val moved = path.moveNode(1, SwipePoint(45f, 5f))
        assertEquals(path.nodes.first(), moved.nodes.first())
        assertEquals(path.nodes.last(), moved.nodes.last())
        assertTrue(moved.isValid())
    }
}
