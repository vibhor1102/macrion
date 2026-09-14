/*
 * Copyright (C) 2026 Vibhor Goel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class InstallationSourceTest {

    @Test
    fun `fdroid installer returns FDROID`() {
        val result = resolveInstallationSource(
            installingPackage = "org.fdroid.fdroid",
            initiatingPackage = "org.fdroid.fdroid",
            originatingPackage = null,
        )
        assertEquals(InstallationSource.FDROID, result)
    }

    @Test
    fun `droidify or other fdroid client returns FDROID`() {
        val result = resolveInstallationSource(
            installingPackage = "com.looker.droidify",
            initiatingPackage = null,
            originatingPackage = null,
        )
        assertEquals(InstallationSource.FDROID, result)
    }

    @Test
    fun `play store installer returns GOOGLE_PLAY`() {
        val result = resolveInstallationSource(
            installingPackage = "com.android.vending",
            initiatingPackage = "com.android.vending",
            originatingPackage = null,
        )
        assertEquals(InstallationSource.GOOGLE_PLAY, result)
    }

    @Test
    fun `adb install with only shell initiator returns UNKNOWN`() {
        val result = resolveInstallationSource(
            installingPackage = null,
            initiatingPackage = "com.android.shell",
            originatingPackage = null,
        )
        assertEquals(InstallationSource.UNKNOWN, result)
    }

    @Test
    fun `null packages returns UNKNOWN`() {
        val result = resolveInstallationSource(
            installingPackage = null,
            initiatingPackage = null,
            originatingPackage = null,
        )
        assertEquals(InstallationSource.UNKNOWN, result)
    }

    @Test
    fun `sideload via packageinstaller returns GITHUB`() {
        val result = resolveInstallationSource(
            installingPackage = "com.google.android.packageinstaller",
            initiatingPackage = "com.android.chrome",
            originatingPackage = null,
            isBrowser = { it == "com.android.chrome" },
        )
        assertEquals(InstallationSource.GITHUB, result)
    }

    @Test
    fun `download manager initiated sideload returns GITHUB`() {
        val result = resolveInstallationSource(
            installingPackage = "com.android.packageinstaller",
            initiatingPackage = "com.android.providers.downloads",
            originatingPackage = null,
        )
        assertEquals(InstallationSource.GITHUB, result)
    }

    @Test
    fun `github app initiated sideload returns GITHUB`() {
        val result = resolveInstallationSource(
            installingPackage = "com.android.packageinstaller",
            initiatingPackage = "com.github.android",
            originatingPackage = null,
        )
        assertEquals(InstallationSource.GITHUB, result)
    }
}
