package io.github.vibhor1102.macrion.core.processing.tests

import android.graphics.Point
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vibhor1102.macrion.core.domain.model.action.PositionCounters
import io.github.vibhor1102.macrion.core.processing.data.processor.resolvePosition
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [29])
class PositionResolverTests {
    @Test fun eachAxisCanUseEitherACounterOrItsLiteral() {
        assertEquals(Point(123, 40), resolvePosition(Point(20, 40), PositionCounters(x = "X"), { 123.9 }))
        assertEquals(Point(123, 456), resolvePosition(null, PositionCounters("X", "Y"), { if (it == "X") 123.0 else 456.0 }))
        assertEquals(Point(-3, 5), resolvePosition(Point(0, 5), PositionCounters(x = "X"), { -3.0 }, allowNegative = true))
    }

    @Test fun unknownOverflowAndNonFiniteValuesNeverBecomeScreenCoordinates() {
        assertNull(resolvePosition(Point(20, 40), PositionCounters(x = "Missing"), { null }))
        listOf(Double.NaN, Double.POSITIVE_INFINITY, -1.0, Int.MAX_VALUE.toDouble() + 1).forEach { value ->
            assertNull(resolvePosition(Point(20, 40), PositionCounters(x = "X"), { value }))
        }
        assertNull(resolvePosition(null, PositionCounters(x = "X"), { 1.0 }))
    }
}
