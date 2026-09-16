/*
 * Copyright (C) 2026 Vibhor Goel
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.vibhor1102.macrion.crash

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import io.github.vibhor1102.macrion.core.base.crash.CrashReportFactory
import io.github.vibhor1102.macrion.core.base.crash.NativeExitData
import io.github.vibhor1102.macrion.core.base.crash.NativeTraceParser
import org.json.JSONObject
import java.io.File

data class HistoricalExit(
    val timestamp: Long,
    val reason: Int,
    val status: Int,
    val importance: Int,
    val pssKb: Long,
    val rssKb: Long,
)

data class HistoricalExitSelection(val checkpoint: Long, val nativeCrash: HistoricalExit?)

object NativeCrashHistory {
    fun select(previousCheckpoint: Long?, exits: List<HistoricalExit>, now: Long): HistoricalExitSelection {
        val latestTimestamp = exits.maxOfOrNull { it.timestamp } ?: 0L
        if (previousCheckpoint == null) return HistoricalExitSelection(maxOf(now, latestTimestamp), null)
        val native = exits.asSequence()
            .filter { it.reason == ApplicationExitInfo.REASON_CRASH_NATIVE && it.timestamp > previousCheckpoint }
            .maxByOrNull { it.timestamp }
        return HistoricalExitSelection(maxOf(previousCheckpoint, latestTimestamp), native)
    }
}

/** Detects Android-recorded native exits on API 30+. Reads only allowlisted native trace fields on API 31+. */
fun Context.captureHistoricalNativeCrash() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
    runCatching { captureHistoricalNativeCrashApi30() }
}

@RequiresApi(Build.VERSION_CODES.R)
private fun Context.captureHistoricalNativeCrashApi30() {
    val checkpointFile = File(noBackupFilesDir, "native-exit-checkpoint")
    val previous = checkpointFile.takeIf { it.isFile }?.readText()?.toLongOrNull()
    val manager = getSystemService(ActivityManager::class.java) ?: return
    val rawExits = manager.getHistoricalProcessExitReasons(null, 0, MAX_EXITS)
    val exits = rawExits.map {
        HistoricalExit(it.timestamp, it.reason, it.status, it.importance, it.pss, it.rss)
    }
    val selection = NativeCrashHistory.select(previous, exits, System.currentTimeMillis())
    val native = selection.nativeCrash
    if (native != null) {
        val alreadyCaptured = crashReportStore().pending().any { report ->
            runCatching {
                JSONObject(report.body).getJSONObject("crash").optJSONObject("nativeExit")
                    ?.optLong("occurredAtMs", -1L) == native.timestamp
            }.getOrDefault(false)
        }
        if (!alreadyCaptured) {
            val trace = if (Build.VERSION.SDK_INT >= 31) runCatching {
                rawExits.firstOrNull { it.timestamp == native.timestamp }?.traceInputStream?.use(NativeTraceParser::parse)
            }.getOrNull() else null
            crashReportStore().save(CrashReportFactory().createNativeExit(
                NativeExitData(native.timestamp, native.status, native.importance, native.pssKb, native.rssKb, trace),
                crashEnvironment(),
            ))
        }
    }
    writeCheckpoint(checkpointFile, selection.checkpoint)
}

private fun writeCheckpoint(target: File, value: Long) {
    val temporary = File(target.parentFile, "${target.name}.tmp")
    try {
        temporary.outputStream().use { output ->
            output.write(value.toString().toByteArray(Charsets.US_ASCII))
            output.fd.sync()
        }
        if (target.exists()) check(target.delete())
        check(temporary.renameTo(target))
    } finally {
        temporary.delete()
    }
}

private const val MAX_EXITS = 16
