/* Copyright (C) 2026 Vibhor Goel; SPDX-License-Identifier: GPL-3.0-or-later */
package io.github.vibhor1102.macrion.core.base.crash

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class ExpandedDiagnosticsTest {
    @Test fun `ring stays bounded and coalesces hot action loops`() {
        repeat(300) {
            CrashDiagnostics.record(if (it % 2 == 0) CrashDiagnostics.Event.CLICK else CrashDiagnostics.Event.SWIPE)
        }
        assertEquals(128, CrashDiagnostics.snapshot().size)
        repeat(1000) { CrashDiagnostics.record(CrashDiagnostics.Event.SET_TEXT) }
        assertEquals(1000, CrashDiagnostics.snapshot().last().count)
        assertNull(CrashDiagnostics.snapshot().last().component)
    }

    @Test fun `native parser keeps crashing thread symbols and drops private fields`() {
        val frame = field(1, 123) + text(4, "detect") + text(6, "/data/app/private/lib/arm64/libmacrion.so") + text(8, "aabbcc")
        val thread = text(2, "private thread name") + bytes(4, frame) + bytes(5, "private memory".toByteArray())
        val entry = field(1, 42) + bytes(2, thread)
        val tombstone = field(6, 42) + text(9, "private command line") + bytes(16, entry) + text(14, "private abort message")
        val parsed = NativeTraceParser.parse(ByteArrayInputStream(tombstone))!!
        assertEquals("libmacrion.so", parsed.frames.single().library)
        assertEquals("7b", parsed.frames.single().relativePc)
        assertEquals("detect", parsed.frames.single().function)
        assertFalse(parsed.toString().contains("private"))
    }

    @Test fun `native library names obey server length bound`() {
        val frame = text(6, "a".repeat(128) + ".so")
        val entry = field(1, 42) + bytes(2, bytes(4, frame))
        val parsed = NativeTraceParser.parse(ByteArrayInputStream(field(6, 42) + bytes(16, entry)))!!
        assertEquals("unknown", parsed.frames.single().library)
    }

    @Test fun `malformed native lengths and oversized inputs fail safely`() {
        assertNull(NativeTraceParser.parse(ByteArrayInputStream(byteArrayOf(0x82.toByte(), 1, 100, 1))))
        assertNull(NativeTraceParser.parse(ByteArrayInputStream(ByteArray(4 * 1024 * 1024 + 1))))
    }

    private fun varint(value: Int): ByteArray {
        var n = value
        val output = ByteArrayOutputStream()
        do { val b = n and 127; n = n ushr 7; output.write(b or if (n > 0) 128 else 0) } while (n > 0)
        return output.toByteArray()
    }
    private fun field(id: Int, value: Int) = varint(id shl 3) + varint(value)
    private fun bytes(id: Int, value: ByteArray) = varint((id shl 3) or 2) + varint(value.size) + value
    private fun text(id: Int, value: String) = bytes(id, value.toByteArray())
}
