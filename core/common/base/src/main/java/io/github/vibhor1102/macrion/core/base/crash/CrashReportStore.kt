/*
 * Copyright (C) 2026 Vibhor Goel
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.vibhor1102.macrion.core.base.crash

import org.json.JSONObject
import java.io.File
import java.util.UUID

/** Caller supplies a dedicated directory under Context.noBackupFilesDir. No upload/approval state exists. */
class CrashReportStore(private val directory: File, private val now: () -> Long = System::currentTimeMillis) {
    data class Report(val id: String, val createdAt: Long, val body: String, val prompted: Boolean)

    fun save(body: String): String = synchronized(LOCK) {
        require(body.toByteArray(Charsets.UTF_8).size <= MAX_BYTES)
        val id = JSONObject(body).getString("reportId")
        require(validId(id))
        check(directory.isDirectory || directory.mkdirs())
        val target = File(directory, "$id.json")
        require(!target.exists()) { "Report already exists" }
        val temporary = File(directory, "$id.tmp")
        try {
            temporary.outputStream().use { out ->
                out.write(body.toByteArray(Charsets.UTF_8))
                out.fd.sync()
            }
            check(temporary.renameTo(target))
            target.setLastModified(now())
        } finally {
            temporary.delete()
        }
        pending()
        id
    }

    fun pending(): List<Report> = synchronized(LOCK) {
        val reports = directory.listFiles().orEmpty().filter { it.extension == "json" }.mapNotNull { file ->
            val id = file.nameWithoutExtension
            if (!validId(id)) return@mapNotNull null
            val report = runCatching {
                require(file.length() <= MAX_BYTES && now() - file.lastModified() <= MAX_AGE_MS)
                val body = file.readText()
                val json = JSONObject(body)
                require(json.getString("reportId") == id && json.getInt("schemaVersion") in 1..2)
                Report(id, file.lastModified(), body, File(directory, "$id.seen").exists())
            }.getOrNull()
            if (report == null) delete(id)
            report
        }.sortedByDescending { it.createdAt }
        reports.drop(3).forEach { delete(it.id) }
        directory.listFiles().orEmpty().filter { it.extension == "tmp" ||
            (it.extension == "seen" && !File(directory, "${it.nameWithoutExtension}.json").exists())
        }.forEach { it.delete() }
        reports.take(3)
    }

    fun markPrompted(id: String) = synchronized(LOCK) {
        require(validId(id))
        if (File(directory, "$id.json").exists()) File(directory, "$id.seen").createNewFile()
        Unit
    }

    fun delete(id: String) = synchronized(LOCK) {
        require(validId(id))
        File(directory, "$id.json").let { check(!it.exists() || it.delete()) }
        File(directory, "$id.seen").let { check(!it.exists() || it.delete()) }
        Unit
    }

    companion object {
        const val MAX_BYTES = 256 * 1024
        const val MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000
        private val LOCK = Any()
        private fun validId(id: String) = runCatching { UUID.fromString(id).toString() == id }.getOrDefault(false)
    }
}
