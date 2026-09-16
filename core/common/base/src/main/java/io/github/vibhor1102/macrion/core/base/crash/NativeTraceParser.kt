/* Copyright (C) 2026 Vibhor Goel; SPDX-License-Identifier: GPL-3.0-or-later */
package io.github.vibhor1102.macrion.core.base.crash

import java.io.InputStream

/** AOSP debuggerd/proto/tombstone.proto: extract only the crashing thread's symbolication fields.
 * Unknown fields (including logs, memory, registers, names, paths and command lines) are skipped.
 * Runs only for a newly observed native crash, never during normal scenario execution. */
object NativeTraceParser {
    data class Frame(val library: String, val function: String?, val relativePc: String,
        val functionOffset: String, val buildId: String?)
    data class Trace(val signal: Int, val code: Int, val frames: List<Frame>)
    private const val MAX_BYTES = 4 * 1024 * 1024

    fun parse(input: InputStream): Trace? = runCatching {
        val bytes = input.readBytesBounded()
        val root = Wire(bytes, 0, bytes.size)
        var tid = 0L
        var signal = 0
        var code = 0
        val threads = ArrayList<Wire>()
        root.fields { field, wire ->
            when {
                field == 6 && wire == 0 -> tid = root.number()
                field == 10 && wire == 2 -> {
                    val s = root.message()
                    s.fields { f, w ->
                        when {
                            f == 1 && w == 0 -> signal = s.number().toInt()
                            f == 3 && w == 0 -> code = s.number().toInt()
                            else -> s.skip(w)
                        }
                    }
                }
                field == 16 && wire == 2 -> {
                    val entry = root.message()
                    if (threads.size < 256) threads.add(entry)
                }
                else -> root.skip(wire)
            }
        }
        var frames: List<Frame> = emptyList()
        for (entry in threads) {
            var id = 0L
            var thread: Wire? = null
            entry.fields { f, w ->
                when {
                    f == 1 && w == 0 -> id = entry.number()
                    f == 2 && w == 2 -> thread = entry.message()
                    else -> entry.skip(w)
                }
            }
            if (id != tid) continue
            val result = ArrayList<Frame>()
            thread?.let { t ->
                t.fields { f, w ->
                    if (f == 4 && w == 2) {
                        val frame = t.message()
                        if (result.size < 64) result.add(readFrame(frame))
                    } else t.skip(w)
                }
            }
            frames = result
            break
        }
        Trace(signal, code, frames)
    }.getOrNull()

    private fun readFrame(frame: Wire): Frame {
        var library = "unknown"
        var function: String? = null
        var buildId: String? = null
        var pc = 0L
        var offset = 0L
        frame.fields { f, w ->
            when {
                f == 1 && w == 0 -> pc = frame.number()
                f == 5 && w == 0 -> offset = frame.number()
                f == 4 && w == 2 -> function = CrashRedactor().redact(frame.text(), 256)
                f == 6 && w == 2 -> {
                    val name = frame.text().substringAfterLast('/').substringAfterLast('!')
                    library = if (name.length <= 128 && name.matches(Regex("[A-Za-z0-9_.+-]{1,128}\\.(so|apk|oat|odex)"))) name else "unknown"
                }
                f == 8 && w == 2 -> buildId = frame.text().takeIf { it.matches(Regex("[a-fA-F0-9]{1,128}")) }
                else -> frame.skip(w)
            }
        }
        return Frame(library, function, java.lang.Long.toUnsignedString(pc, 16),
            java.lang.Long.toUnsignedString(offset, 16), buildId)
    }

    private fun InputStream.readBytesBounded(): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val block = ByteArray(8192)
        while (true) {
            val count = read(block)
            if (count < 0) break
            require(output.size() + count <= MAX_BYTES)
            output.write(block, 0, count)
        }
        return output.toByteArray()
    }

    private class Wire(val bytes: ByteArray, var position: Int, val end: Int) {
        fun number(): Long {
            var value = 0L
            for (shift in 0..63 step 7) {
                require(position < end)
                val b = bytes[position++].toInt() and 255
                if (shift == 63) require(b <= 1)
                value = value or ((b and 127).toLong() shl shift)
                if (b < 128) return value
            }
            error("Invalid varint")
        }
        fun message(): Wire {
            val length = number()
            require(length >= 0 && length <= end - position)
            return Wire(bytes, position, position + length.toInt()).also { position = it.end }
        }
        fun text(): String = message().let { String(bytes, it.position, minOf(it.end - it.position, 512), Charsets.UTF_8) }
        fun fields(block: (Int, Int) -> Unit) {
            while (position < end) {
                val key = number()
                require(key > 0 && key <= Int.MAX_VALUE)
                block((key ushr 3).toInt(), (key and 7).toInt())
            }
        }
        fun skip(wire: Int) {
            when (wire) {
                0 -> number()
                1 -> { require(end - position >= 8); position += 8 }
                2 -> message()
                5 -> { require(end - position >= 4); position += 4 }
                else -> error("Unsupported protobuf wire type")
            }
        }
    }
}
