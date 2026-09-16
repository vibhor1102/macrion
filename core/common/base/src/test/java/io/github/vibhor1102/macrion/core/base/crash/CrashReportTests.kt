/*
 * Copyright (C) 2026 Vibhor Goel
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.vibhor1102.macrion.core.base.crash

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class CrashReportTests {
    @get:Rule val temporary = TemporaryFolder()
    private val environment = CrashEnvironment("0.2.0", 20002, "fDroid", "debug", "14", 34, "Test", "Model", "arm64-v8a")

    @Test fun `redacts secrets in nested exceptions while preserving technical details`() {
        val cause = IllegalArgumentException("index 3 tester@example.com https://host/path?token=secret /storage/emulated/0/personal.txt")
        val error = IllegalStateException("token=private-value password=hidden", cause)
        error.addSuppressed(RuntimeException("Authorization: Bearer abc.def.ghi"))
        val report = CrashReportFactory().create(error, environment, true)
        listOf("tester@example.com", "personal.txt", "private-value", "hidden", "abc.def.ghi", "host/path").forEach {
            assertFalse("Report leaked $it", report.contains(it))
        }
        assertTrue(report.contains("index 3"))
        assertTrue(report.contains("IllegalArgumentException"))
        assertTrue(JSONObject(report).getInt("redactionCount") >= 5)
    }

    @Test fun `retains typed detection context on original exception`() {
        val error = IllegalArgumentException("Detection failed")
        try {
            error.throwWithContext(DetectionCrashContext(DetectionCrashContext.Operation.TEXT,
                1080, 2400, 0, 0, 100, 100, 80, textLength = 12, modelId = "latin"))
        } catch (caught: IllegalArgumentException) {
            assertSame(error, caught)
        }
        val detection = JSONObject(CrashReportFactory().create(error, environment, false))
            .getJSONObject("crash").getJSONObject("detection")
        assertEquals(12, detection.getInt("textLength"))
        assertFalse(detection.has("conditionText"))
        assertEquals("latin", detection.getString("modelId"))
    }

    @Test fun `bounds cyclic and huge reports and leaves valid JSON`() {
        val one = RuntimeException("x".repeat(500_000))
        val two = RuntimeException("cause", one)
        one.initCause(two)
        one.stackTrace = Array(2000) { StackTraceElement("a".repeat(500), "b".repeat(500), "c".repeat(500), 42) }
        val body = CrashReportFactory().create(one, environment, false)
        assertTrue(body.toByteArray().size <= CrashReportStore.MAX_BYTES)
        assertTrue(JSONObject(body).getBoolean("truncated"))
    }

    @Test fun `retains only three reports expires old ones and preserves preview bytes`() {
        var now = 1_700_000_000_000L
        val store = CrashReportStore(temporary.newFolder()) { now }
        repeat(4) {
            store.save(CrashReportFactory().create(RuntimeException("failure $it"), environment, false))
            now++
        }
        assertEquals(3, store.pending().size)
        val latest = store.pending().first()
        store.markPrompted(latest.id)
        assertEquals(latest.body, store.pending().first().body)
        assertTrue(store.pending().first().prompted)
        now += CrashReportStore.MAX_AGE_MS + 1
        assertTrue(store.pending().isEmpty())
    }

    @Test fun `discard removes report and marker and corrupt files are ignored`() {
        val dir = temporary.newFolder()
        val store = CrashReportStore(dir)
        val id = store.save(CrashReportFactory().create(RuntimeException("test"), environment, true))
        store.markPrompted(id)
        store.delete(id)
        assertTrue(store.pending().isEmpty())
        assertTrue(dir.listFiles()!!.isEmpty())
        File(dir, "$id.json").writeText("invalid JSON")
        File(dir, "$id.tmp").writeText("interrupted")
        assertTrue(store.pending().isEmpty())
        assertTrue(dir.listFiles()!!.isEmpty())
    }

    @Test fun `storage preserves pending v1 and v2 reports together`() {
        val store = CrashReportStore(temporary.newFolder())
        val old = JSONObject(CrashReportFactory().create(RuntimeException("old"), environment, true))
        old.put("schemaVersion", 1).remove("recentErrors")
        old.put("recentOperations", org.json.JSONArray())
        store.save(old.toString())
        store.save(CrashReportFactory().create(RuntimeException("new"), environment, true))
        assertEquals(setOf(1, 2), store.pending().map { JSONObject(it.body).getInt("schemaVersion") }.toSet())
    }

    @Test fun `caught diagnostics preserve redaction and truncation accounting`() {
        val error = IllegalArgumentException("token=private " + "x".repeat(3000))
        error.stackTrace = Array(20) { StackTraceElement("TechnicalClass", "execute", "Source.kt", it) }
        CrashDiagnostics.recordFailure(error)
        val json = JSONObject(CrashReportFactory().create(RuntimeException("failure"), environment, true))
        val errors = json.getJSONArray("recentErrors")
        val caught = errors.getJSONObject(errors.length() - 1)
        assertEquals(16, caught.getJSONArray("frames").length())
        assertFalse(caught.getString("message").contains("private"))
        assertTrue(json.getInt("redactionCount") > 0)
        assertTrue(json.getBoolean("truncated"))
    }

    @Test fun `rejects path traversal ids`() {
        val store = CrashReportStore(temporary.newFolder())
        assertThrows(IllegalArgumentException::class.java) { store.delete("../outside") }
        assertThrows(IllegalArgumentException::class.java) { store.save("{\"reportId\":\"../outside\"}") }
    }

    @Test fun `native exit report contains minimal bounded metadata without a tombstone`() {
        val json = JSONObject(CrashReportFactory().createNativeExit(
            NativeExitData(1_700_000_000_000L, 11, 100, 1234, 5678), environment))
        val crash = json.getJSONObject("crash")
        assertEquals("android.native_crash", crash.getString("type"))
        assertEquals(11, crash.getJSONObject("nativeExit").getInt("status"))
        assertEquals(0, crash.getJSONArray("frames").length())
        assertFalse(crash.has("message"))
        listOf("trace", "tombstone", "description", "processName").forEach { assertFalse(json.toString().contains(it)) }
    }
}
