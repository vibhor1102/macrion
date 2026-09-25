package io.github.vibhor1102.macrion.core.base.gesture

import android.graphics.Path
import android.graphics.Point
import android.graphics.PointF
import androidx.room.TypeConverter
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.hypot
import kotlin.math.roundToInt

/** Geometry of one continuous finger stroke. Timing belongs to the swipe, not its nodes. */
@Serializable
data class SwipePoint(val x: Float, val y: Float) {
    constructor(point: Point) : this(point.x.toFloat(), point.y.toFloat())
    constructor(point: PointF) : this(point.x, point.y)

    fun toPoint(): Point = Point(x.roundToInt(), y.roundToInt())
    fun distanceTo(other: SwipePoint): Float = hypot(x - other.x, y - other.y)
}

/** Controls are kept internally; only [position] becomes an editable handle. */
@Serializable
data class SwipeNode(
    val position: SwipePoint,
    val controlIn: SwipePoint? = null,
    val controlOut: SwipePoint? = null,
)

@Serializable
data class SwipePath(val nodes: List<SwipeNode>) {
    val start: SwipePoint get() = nodes.first().position
    val end: SwipePoint get() = nodes.last().position
    val isCurved: Boolean get() = nodes.size > 2 || nodes.any { it.controlIn != null || it.controlOut != null }

    /** Move one anchor and its hidden controls, leaving all other anchors in place. */
    fun moveNode(index: Int, position: SwipePoint): SwipePath {
        if (index !in nodes.indices) return this
        val old = nodes[index]
        val dx = position.x - old.position.x
        val dy = position.y - old.position.y
        fun SwipePoint.shifted() = SwipePoint((x + dx).coerceAtLeast(0f), (y + dy).coerceAtLeast(0f))
        return copy(nodes = nodes.toMutableList().also { updated ->
            updated[index] = old.copy(
                position = position,
                controlIn = old.controlIn?.shifted(),
                controlOut = old.controlOut?.shifted(),
            )
        })
    }

    fun isValid(): Boolean = nodes.size >= 2 && nodes.all { node ->
        listOfNotNull(node.position, node.controlIn, node.controlOut).all { point ->
            point.x.isFinite() && point.y.isFinite() && point.x >= 0f && point.y >= 0f
        }
    } && nodes.zipWithNext().all { (from, to) ->
        (from.controlOut == null) == (to.controlIn == null)
    } && nodes.zipWithNext().any { (from, to) ->
        from.position != to.position || from.controlOut != null || to.controlIn != null
    }

    fun toAndroidPath(offsetX: Float = 0f, offsetY: Float = 0f): Path = Path().apply {
        moveTo(start.x + offsetX, start.y + offsetY)
        nodes.zipWithNext().forEach { (from, to) ->
            val firstControl = from.controlOut
            val secondControl = to.controlIn
            if (firstControl != null && secondControl != null) {
                cubicTo(
                    firstControl.x + offsetX, firstControl.y + offsetY,
                    secondControl.x + offsetX, secondControl.y + offsetY,
                    to.position.x + offsetX, to.position.y + offsetY,
                )
            } else {
                lineTo(to.position.x + offsetX, to.position.y + offsetY)
            }
        }
    }

    companion object {
        fun line(from: SwipePoint, to: SwipePoint): SwipePath =
            SwipePath(listOf(SwipeNode(from), SwipeNode(to)))
    }
}

/** Room stores one small path value; native backup serializes [SwipePath] as readable JSON. */
class SwipePathRoomConverter {
    @TypeConverter
    fun fromColumn(value: String?): SwipePath? = value?.let { Json.decodeFromString<SwipePath>(it) }

    @TypeConverter
    fun toColumn(value: SwipePath?): String? = value?.let { Json.encodeToString(it) }
}
