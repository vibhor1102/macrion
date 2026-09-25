package io.github.vibhor1102.macrion.core.base.gesture

import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Fits a continuous trace with as few editable anchors as its shape permits. Every retained
 * anchor is editable; the Bézier controls between anchors are deliberately hidden from the UI.
 * The tolerance is geometric only. No movement timestamps are stored in the result.
 */
fun fitSwipePath(rawPoints: List<SwipePoint>, tolerancePx: Float = 4f): SwipePath? {
    if (rawPoints.size < 2) return null
    val points = buildList<SwipePoint> {
        rawPoints.forEachIndexed { index, point ->
            if (index == 0 || index == rawPoints.lastIndex || isEmpty() || last().distanceTo(point) >= 1f) {
                add(point)
            }
        }
    }
    if (points.size < 2) return null
    val anchors = mutableListOf(0, points.lastIndex)
    val tolerance = tolerancePx.coerceAtLeast(1f)

    while (true) {
        val path = fitSegments(points, anchors, tolerance)
        var worstError = tolerance
        var worstIndex = -1
        anchors.zipWithNext().forEachIndexed { segmentIndex, (first, last) ->
            val from = path.nodes[segmentIndex]
            val to = path.nodes[segmentIndex + 1]
            for (index in first + 1 until last) {
                val error = distanceToSegment(points[index], from, to)
                if (error > worstError) {
                    worstError = error
                    worstIndex = index
                }
            }
        }
        if (worstIndex < 0) return path.takeIf(SwipePath::isValid)
        anchors.add(worstIndex)
        anchors.sort()
    }
}

private fun fitSegments(points: List<SwipePoint>, anchors: List<Int>, tolerance: Float): SwipePath {
    val nodes = anchors.map { SwipeNode(points[it]) }.toMutableList()
    anchors.zipWithNext().forEachIndexed { segmentIndex, (first, last) ->
        val start = points[first]
        val end = points[last]
        val chord = start.distanceTo(end)
        val lineError = (first + 1 until last).maxOfOrNull { pointIndex ->
            distanceToLineSegment(points[pointIndex], start, end)
        } ?: 0f
        if (lineError <= tolerance * 0.5f || chord < 1f) return@forEachIndexed

        val outgoing = direction(start, points[(first + 1).coerceAtMost(points.lastIndex)])
        val incoming = direction(points[(last - 1).coerceAtLeast(0)], end)
        val controlLength = chord / 3f
        val controlOut = SwipePoint(
            (start.x + outgoing.first * controlLength).coerceAtLeast(0f),
            (start.y + outgoing.second * controlLength).coerceAtLeast(0f),
        )
        val controlIn = SwipePoint(
            (end.x - incoming.first * controlLength).coerceAtLeast(0f),
            (end.y - incoming.second * controlLength).coerceAtLeast(0f),
        )
        nodes[segmentIndex] = nodes[segmentIndex].copy(controlOut = controlOut)
        nodes[segmentIndex + 1] = nodes[segmentIndex + 1].copy(controlIn = controlIn)
    }
    return SwipePath(nodes)
}

private fun direction(from: SwipePoint, to: SwipePoint): Pair<Float, Float> {
    val distance = from.distanceTo(to)
    return if (distance < 0.001f) 0f to 0f else (to.x - from.x) / distance to (to.y - from.y) / distance
}

private fun distanceToSegment(point: SwipePoint, from: SwipeNode, to: SwipeNode): Float {
    val firstControl = from.controlOut
    val secondControl = to.controlIn
    if (firstControl == null || secondControl == null) {
        return distanceToLineSegment(point, from.position, to.position)
    }
    var smallest = Float.POSITIVE_INFINITY
    var previous = from.position
    for (step in 1..24) {
        val current = cubic(from.position, firstControl, secondControl, to.position, step / 24f)
        smallest = min(smallest, distanceToLineSegment(point, previous, current))
        previous = current
    }
    return smallest
}

private fun cubic(a: SwipePoint, b: SwipePoint, c: SwipePoint, d: SwipePoint, t: Float): SwipePoint {
    val u = 1f - t
    val aa = u * u * u
    val bb = 3f * u * u * t
    val cc = 3f * u * t * t
    val dd = t * t * t
    return SwipePoint(
        aa * a.x + bb * b.x + cc * c.x + dd * d.x,
        aa * a.y + bb * b.y + cc * c.y + dd * d.y,
    )
}

private fun distanceToLineSegment(point: SwipePoint, from: SwipePoint, to: SwipePoint): Float {
    val dx = to.x - from.x
    val dy = to.y - from.y
    val denominator = dx * dx + dy * dy
    if (denominator < 0.001f) return point.distanceTo(from)
    val t = max(0f, min(1f, ((point.x - from.x) * dx + (point.y - from.y) * dy) / denominator))
    return hypot(point.x - from.x - t * dx, point.y - from.y - t * dy)
}
