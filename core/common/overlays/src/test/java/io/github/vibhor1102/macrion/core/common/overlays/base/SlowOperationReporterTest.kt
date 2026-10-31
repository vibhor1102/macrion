package io.github.vibhor1102.macrion.core.common.overlays.base

import org.junit.Assert.assertEquals
import org.junit.Test

class SlowOperationReporterTest {
    @Test fun fastOperationsDoNotSuppressTheFirstSlowWarning() {
        val warnings = mutableListOf<Long>()
        val reporter = SlowOperationReporter { _, _, duration -> warnings.add(duration) }
        reporter.report(999, 100_000, "Overlay", "create")
        reporter.report(1_000, 100_001, "Overlay", "start")
        assertEquals(listOf(1_000L), warnings)
    }

    @Test fun nestedAndRepeatedSlowOperationsShareTheRateLimit() {
        val warnings = mutableListOf<String>()
        val reporter = SlowOperationReporter { component, operation, _ -> warnings.add("$component.$operation") }
        reporter.report(1_200, 2_000, "Child", "create")
        reporter.report(2_000, 2_001, "Parent", "rotate")
        reporter.report(1_500, 61_999, "Other", "start")
        reporter.report(1_001, 62_000, "Other", "start")
        assertEquals(listOf("Child.create", "Other.start"), warnings)
    }
}
