/* Copyright (C) 2026 Vibhor Goel; SPDX-License-Identifier: GPL-3.0-or-later */
package io.github.vibhor1102.macrion.crash

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class CrashUpdateCheckerTests {
    private val checker = CrashUpdateChecker { error("No network in parser tests") }

    @Test fun `stable tags compare numerically and ignore malformed or prerelease versions`() {
        assertTrue(isNewerStableVersion("v0.6.10", "0.6.9"))
        assertTrue(isNewerStableVersion("v1.0.0", "0.99.99"))
        for (version in listOf("v0.6.9", "v0.6.8", "v0.6.10-beta1", "latest", "0.6", "999999999999999999999999.0.0")) {
            assertFalse(version, isNewerStableVersion(version, "0.6.9"))
        }
        assertFalse(isNewerStableVersion("v0.6.10", "0.6.9-dev"))
    }

    @Test fun `github requires stable published release and compatible apk`() {
        val body = """{"tag_name":"v0.6.10","draft":false,"prerelease":false,
            "assets":[{"name":"macrion-0.6.10-arm64-v8a.apk"}]}"""
        assertEquals("https://github.com/vibhor1102/Macrion/releases/tag/v0.6.10",
            checker.github(body, "0.6.9", listOf("arm64-v8a"))?.url)
        assertNull(checker.github(body, "0.6.10", listOf("arm64-v8a")))
        assertNull(checker.github(body, "0.6.9", listOf("x86")))
        assertNull(checker.github(body.replace("\"draft\":false", "\"draft\":true"), "0.6.9", listOf("arm64-v8a")))
        assertNull(checker.github(body.replace("\"prerelease\":false", "\"prerelease\":true"), "0.6.9", listOf("arm64-v8a")))
        assertNotNull(checker.github(body.replace("arm64-v8a.apk", "universal.apk"), "0.6.9", listOf("x86")))
    }

    @Test fun `fdroid uses published compatible version within suggested release`() {
        val body = fdroid("""{"versionCode":150002,"versionName":"0.6.4"},
            {"versionCode":150004,"versionName":"0.6.4"},
            {"versionCode":160002,"versionName":"0.6.5"}""")
        assertEquals("0.6.4", checker.fdroid(body, 140002, listOf("arm64-v8a"))?.version)
        assertNull(checker.fdroid(body, 150002, listOf("arm64-v8a")))
        assertNull(checker.fdroid(body, 140001, listOf("armeabi-v7a")))
        assertNull(checker.fdroid(body, 160002, listOf("arm64-v8a")))
    }

    @Test fun `fdroid never mistakes architecture suffix or unpublished suggestion for update`() {
        assertNull(checker.fdroid(fdroid("""{"versionCode":140004,"versionName":"0.6.3"}"""), 140002, listOf("x86_64")))
        assertNull(checker.fdroid(fdroid("""{"versionCode":150004,"versionName":"0.6.4"}"""), 140002, listOf("arm64-v8a")))
        assertNull(checker.fdroid(fdroid("""{"versionCode":150002,"versionName":"0.6.4-beta1"}"""), 140002, listOf("arm64-v8a")))
        assertNull(checker.fdroid(fdroid("""{"versionCode":150000,"versionName":"0.6.4"}""").replace(CrashUpdateChecker.PACKAGE_NAME, "other.app"), 140002, listOf("arm64-v8a")))
        assertNotNull(checker.fdroid(fdroid("""{"versionCode":150000,"versionName":"0.6.4"}"""), 140002, listOf("arm64-v8a")))
    }

    private fun fdroid(packages: String) = """{"packageName":"${CrashUpdateChecker.PACKAGE_NAME}",
        "suggestedVersionCode":150004,"packages":[$packages]}"""
}
