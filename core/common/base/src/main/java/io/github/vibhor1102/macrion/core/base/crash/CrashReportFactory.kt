/*
 * Copyright (C) 2026 Vibhor Goel
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.vibhor1102.macrion.core.base.crash

import org.json.JSONArray
import org.json.JSONObject
import java.util.Collections
import java.util.IdentityHashMap
import java.util.UUID

data class CrashEnvironment(
    val versionName: String, val versionCode: Long, val flavor: String, val buildType: String,
    val androidVersion: String, val api: Int, val manufacturer: String, val model: String, val abi: String,
)

data class NativeExitData(
    val occurredAtMs: Long,
    val status: Int,
    val importance: Int,
    val pssKb: Long,
    val rssKb: Long,
    val trace: NativeTraceParser.Trace? = null,
)

/** Does not read databases, preferences, logcat, thread names or arbitrary custom data. */
class CrashReportFactory {
    // One extra trace at report creation only. R8 embeds its ID in the source-file attribute.
    private fun currentMappingId(): String? = Throwable().stackTrace.firstNotNullOfOrNull { frame ->
        frame.fileName?.let { Regex("^r8-map-id-([a-f0-9]{7,64})$").matchEntire(it)?.groupValues?.get(1) }
    }

    fun create(error: Throwable, environment: CrashEnvironment, mainThread: Boolean): String {
        val redactor = CrashRedactor()
        val seen = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
        var remainingFrames = 256
        var remainingExceptions = 16
        var truncated = false
        fun throwable(value: Throwable, depth: Int): JSONObject {
            if (depth >= 8 || remainingExceptions-- <= 0 || !seen.add(value)) {
                truncated = true
                return JSONObject().put("truncated", true)
            }
            val trace = value.stackTrace
            val frames = trace.take(remainingFrames)
            remainingFrames -= frames.size
            if (trace.size > frames.size) truncated = true
            val result = JSONObject()
                .put("type", redactor.redact(value.javaClass.name, 256))
                .put("message", redactor.redact(value.message, 8192))
                .put("frames", JSONArray(frames.map { frame ->
                    JSONObject().put("class", redactor.redact(frame.className, 256))
                        .put("method", redactor.redact(frame.methodName, 256))
                        .put("file", redactor.redact(frame.fileName, 128))
                        .put("line", frame.lineNumber).put("native", frame.isNativeMethod)
                }))
            value.cause?.let { result.put("cause", throwable(it, depth + 1)) }
            if (value.suppressed.size > 4) truncated = true
            result.put("suppressed", JSONArray(value.suppressed.take(4).map { throwable(it, depth + 1) }))
            CrashDiagnostics.context(value)?.let { context ->
                result.put("detection", JSONObject()
                    .put("operation", context.operation.name)
                    .put("screenWidth", context.screenWidth).put("screenHeight", context.screenHeight)
                    .put("areaLeft", context.areaLeft).put("areaTop", context.areaTop)
                    .put("areaWidth", context.areaWidth).put("areaHeight", context.areaHeight)
                    .put("threshold", context.threshold).put("textLength", context.textLength)
                    .put("modelId", redactor.redact(context.modelId, 128))
                    .put("originalWidth", context.originalWidth).put("originalHeight", context.originalHeight)
                    .put("scaledWidth", context.scaledWidth).put("scaledHeight", context.scaledHeight)
                    .put("color", context.color).put("numberFormat", context.numberFormat))
            }
            return result
        }
        val now = System.nanoTime()
        val crash = throwable(error, 0)
        val recentErrors = CrashDiagnostics.errorSnapshot()
        val previousRedactions = recentErrors.sumOf { it.redactionCount }
        val root = JSONObject().put("schemaVersion", 2).put("redactionVersion", 1)
            .put("reportId", UUID.randomUUID().toString())
            .put("build", JSONObject().put("versionName", redactor.redact(environment.versionName, 128))
                .put("versionCode", environment.versionCode).put("flavor", environment.flavor)
                .put("buildType", environment.buildType).put("mappingId", currentMappingId()))
            .put("device", JSONObject().put("androidVersion", redactor.redact(environment.androidVersion, 128))
                .put("api", environment.api).put("manufacturer", redactor.redact(environment.manufacturer, 128))
                .put("model", redactor.redact(environment.model, 128)).put("abi", redactor.redact(environment.abi, 128)))
            .put("mainThread", mainThread).put("crash", crash)
            .put("recentOperations", JSONArray(CrashDiagnostics.snapshot().map {
                JSONObject().put("event", it.event.name).put("millisecondsBeforeCrash", ((now - it.elapsedNanos) / 1_000_000).coerceAtLeast(0))
                    .put("component", it.component).put("count", it.count).put("state", it.state).put("attached", it.attached)
            }))
            .put("recentErrors", JSONArray(recentErrors.map { error ->
                JSONObject().put("type", error.type).put("message", error.message)
                    .put("millisecondsBeforeCrash", ((now - error.elapsedNanos) / 1_000_000).coerceAtLeast(0))
                    .put("frames", JSONArray(error.frames.map { frame ->
                        JSONObject().put("class", redactor.redact(frame.className, 256))
                            .put("method", redactor.redact(frame.methodName, 256))
                            .put("file", redactor.redact(frame.fileName, 128))
                            .put("line", frame.lineNumber).put("native", frame.isNativeMethod)
                    }))
            }))
            .put("redactionCount", redactor.replacements + previousRedactions)
            .put("truncated", truncated || redactor.truncated || recentErrors.any { it.truncated })
        // JSON escaping can expand bounded strings. Drop bulky frames, never cut serialized JSON.
        if (root.toString().toByteArray(Charsets.UTF_8).size > CrashReportStore.MAX_BYTES) {
            root.put("crash", JSONObject().put("type", redactor.redact(error.javaClass.name, 256))
                .put("message", redactor.redact(error.message, 2048)))
            root.put("truncated", true).put("redactionCount", redactor.replacements + previousRedactions)
        }
        val formatted = root.toString(2)
        return if (formatted.toByteArray(Charsets.UTF_8).size <= CrashReportStore.MAX_BYTES) formatted else root.toString()
    }

    /** Historical report with optional allowlisted native symbols; no raw tombstone data. */
    fun createNativeExit(exit: NativeExitData, environment: CrashEnvironment): String {
        val redactor = CrashRedactor()
        val crash = JSONObject()
            .put("type", "android.native_crash")
            .put("frames", JSONArray())
            .put("suppressed", JSONArray())
            .put("nativeExit", JSONObject()
                .put("occurredAtMs", exit.occurredAtMs)
                .put("status", exit.status)
                .put("importance", exit.importance)
                .put("pssKb", exit.pssKb.coerceAtLeast(0))
                .put("rssKb", exit.rssKb.coerceAtLeast(0)))
        exit.trace?.let { trace ->
            crash.put("nativeTrace", JSONObject().put("signal", trace.signal).put("code", trace.code)
                .put("frames", JSONArray(trace.frames.map { frame ->
                    JSONObject().put("library", frame.library).put("function", frame.function)
                        .put("relativePc", frame.relativePc).put("functionOffset", frame.functionOffset)
                        .put("buildId", frame.buildId)
                })))
        }
        return JSONObject().put("schemaVersion", 2).put("redactionVersion", 1)
            .put("reportId", UUID.randomUUID().toString())
            .put("build", JSONObject().put("versionName", redactor.redact(environment.versionName, 128))
                .put("versionCode", environment.versionCode).put("flavor", environment.flavor)
                .put("buildType", environment.buildType).put("mappingId", currentMappingId()))
            .put("device", JSONObject().put("androidVersion", redactor.redact(environment.androidVersion, 128))
                .put("api", environment.api).put("manufacturer", redactor.redact(environment.manufacturer, 128))
                .put("model", redactor.redact(environment.model, 128)).put("abi", redactor.redact(environment.abi, 128)))
            .put("mainThread", false).put("crash", crash)
            .put("recentOperations", JSONArray())
            .put("redactionCount", redactor.replacements).put("truncated", redactor.truncated)
            .toString(2)
    }
}
