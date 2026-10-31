/* Copyright (C) 2026 Vibhor Goel; SPDX-License-Identifier: GPL-3.0-or-later */
package io.github.vibhor1102.macrion.crash

import android.content.Context
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import io.github.vibhor1102.macrion.settings.InstallationSource
import io.github.vibhor1102.macrion.settings.detectInstallationSource
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

internal data class CrashUpdate(val source: InstallationSource, val version: String, val url: String) {
    val key: String get() = "${source.name}:$version"
}

/** Informational checks only: no report data, APK downloads, credentials, or installation. */
internal class CrashUpdateChecker(
    private val fetch: (String) -> String = ::fetchUpdateJson,
) {
    fun check(context: Context): CrashUpdate? {
        // Debug installs are not a distribution channel, even when installed by a file manager.
        if (context.packageName != PACKAGE_NAME) return null
        @Suppress("DEPRECATION")
        val installed = context.packageManager.getPackageInfo(context.packageName, 0)
        val source = detectInstallationSource(context, allowSideloadGuess = false)
        return when (source) {
            InstallationSource.GITHUB -> github(
                fetch("https://api.github.com/repos/vibhor1102/Macrion/releases/latest"),
                installed.versionName.orEmpty(), Build.SUPPORTED_ABIS.toList(),
            )
            InstallationSource.FDROID -> fdroid(
                fetch("https://f-droid.org/api/v1/packages/$PACKAGE_NAME"),
                PackageInfoCompat.getLongVersionCode(installed), Build.SUPPORTED_ABIS.toList(),
            )
            else -> null
        }
    }

    internal fun github(body: String, installed: String, abis: List<String>): CrashUpdate? {
        val json = JSONObject(body)
        if (json.optBoolean("draft", true) || json.optBoolean("prerelease", true)) return null
        val tag = json.getString("tag_name")
        if (!isNewerStableVersion(tag, installed)) return null
        val version = tag.removePrefix("v")
        val assets = json.getJSONArray("assets")
        val names = (0 until assets.length()).map { assets.getJSONObject(it).getString("name") }.toSet()
        if ((abis + "universal").none { "macrion-$version-$it.apk" in names }) return null
        // Construct our own URL rather than accepting arbitrary destinations from a response.
        return CrashUpdate(InstallationSource.GITHUB, version, "https://github.com/vibhor1102/Macrion/releases/tag/$tag")
    }

    internal fun fdroid(body: String, installedCode: Long, abis: List<String>): CrashUpdate? {
        if (installedCode < 10_000) return null
        val json = JSONObject(body)
        if (json.getString("packageName") != PACKAGE_NAME) return null
        val suggested = json.getLong("suggestedVersionCode")
        val packages = json.getJSONArray("packages")
        val supported = abis.mapNotNull { ABI_CODES[it] }.toSet() + 0L
        // Macrion codes are baseVersionCode * 10_000 + ABI code. Suggested may be x86_64.
        return (0 until packages.length()).mapNotNull { index ->
            val pkg = packages.getJSONObject(index)
            val code = pkg.getLong("versionCode")
            val version = pkg.getString("versionName")
            if (code / 10_000 <= installedCode / 10_000 || code / 10_000 > suggested / 10_000 ||
                code % 10_000 !in supported || stableVersion(version) == null
            ) null else code to CrashUpdate(InstallationSource.FDROID, version, "https://f-droid.org/packages/$PACKAGE_NAME/")
        }.maxByOrNull { it.first }?.second
    }

    companion object {
        const val PACKAGE_NAME = "io.github.vibhor1102.macrion"
        private val ABI_CODES = mapOf("armeabi-v7a" to 1L, "arm64-v8a" to 2L, "x86" to 3L, "x86_64" to 4L)
    }
}

internal fun stableVersion(value: String): List<Long>? {
    if (!Regex("v?[0-9]+\\.[0-9]+\\.[0-9]+").matches(value)) return null
    return value.removePrefix("v").split('.').map { it.toLongOrNull() ?: return null }
}

internal fun isNewerStableVersion(candidate: String, installed: String): Boolean {
    val newer = stableVersion(candidate) ?: return false
    val current = stableVersion(installed) ?: return false
    return newer.zip(current).firstOrNull { (a, b) -> a != b }?.let { (a, b) -> a > b } ?: false
}

private fun fetchUpdateJson(address: String): String {
    val connection = URL(address).openConnection() as HttpURLConnection
    return try {
        connection.connectTimeout = 3_000
        connection.readTimeout = 3_000
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "Macrion-update-check")
        check(connection.responseCode == HttpURLConnection.HTTP_OK)
        connection.inputStream.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8 * 1024)
            val deadline = System.nanoTime() + 3_000_000_000L
            while (true) {
                check(System.nanoTime() < deadline)
                val count = input.read(buffer)
                if (count < 0) break
                check(output.size() + count <= 256 * 1024)
                output.write(buffer, 0, count)
            }
            output.toString(Charsets.UTF_8.name())
        }
    } finally {
        connection.disconnect()
    }
}
