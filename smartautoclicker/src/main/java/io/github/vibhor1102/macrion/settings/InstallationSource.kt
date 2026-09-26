/*
 * Copyright (C) 2026 Vibhor Goel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.vibhor1102.macrion.settings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.StringRes
import androidx.core.net.toUri
import io.github.vibhor1102.macrion.R

enum class InstallationSource(
    val displayName: String? = null,
    @param:StringRes val stringRes: Int? = null,
) {
    FDROID(displayName = "F-Droid"),
    GITHUB(displayName = "GitHub"),
    GOOGLE_PLAY(displayName = "Google Play"),
    UNKNOWN(stringRes = R.string.install_source_unknown);

    fun getLabel(context: Context): String =
        stringRes?.let { context.getString(it) } ?: displayName.orEmpty()
}

fun detectInstallationSource(context: Context): InstallationSource {
    val pm = context.packageManager
    val packageName = context.packageName

    val installingPackage: String?
    val initiatingPackage: String?
    val originatingPackage: String?

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val info = try {
            pm.getInstallSourceInfo(packageName)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        } catch (_: SecurityException) {
            null
        }
        installingPackage = info?.installingPackageName
        initiatingPackage = info?.initiatingPackageName
        originatingPackage = info?.originatingPackageName
    } else {
        @Suppress("DEPRECATION")
        installingPackage = try {
            pm.getInstallerPackageName(packageName)
        } catch (_: Exception) {
            null
        }
        initiatingPackage = null
        originatingPackage = null
    }

    return resolveInstallationSource(
        installingPackage = installingPackage,
        initiatingPackage = initiatingPackage,
        originatingPackage = originatingPackage,
        isBrowser = { pkg -> isBrowserPackage(pm, pkg) },
    )
}

internal fun resolveInstallationSource(
    installingPackage: String?,
    initiatingPackage: String?,
    originatingPackage: String?,
    isBrowser: (String) -> Boolean = { false },
): InstallationSource {
    val packages = listOfNotNull(installingPackage, initiatingPackage, originatingPackage)

    // Check for F-Droid clients
    val isFdroid = packages.any { pkg ->
        pkg == "org.fdroid.fdroid" ||
            pkg == "org.fdroid.fdroid.privileged" ||
            pkg == "org.fdroid.basic" ||
            pkg == "com.aurora.adroid" ||
            pkg == "com.looker.droidify" ||
            pkg == "com.machiav3lli.fdroid" ||
            pkg == "eu.bubu1.fdroidclassic"
    }
    if (isFdroid) return InstallationSource.FDROID

    // Check for Google Play Store
    if (packages.any { it == "com.android.vending" }) {
        return InstallationSource.GOOGLE_PLAY
    }

    // Direct ADB/shell installs have no installer or only shell as initiator
    if (packages.isEmpty() || packages.all { it == "com.android.shell" }) {
        return InstallationSource.UNKNOWN
    }

    // Check for GitHub app or direct browser / package installer download
    val isGitHubOrBrowser = packages.any { pkg ->
        pkg.contains("github", ignoreCase = true) ||
            isBrowser(pkg) ||
            isDownloadOrFileManager(pkg) ||
            isPackageInstaller(pkg)
    }

    return if (isGitHubOrBrowser) InstallationSource.GITHUB else InstallationSource.UNKNOWN
}

private fun isPackageInstaller(pkg: String): Boolean =
    pkg == "com.google.android.packageinstaller" ||
        pkg == "com.android.packageinstaller" ||
        pkg == "com.samsung.android.packageinstaller" ||
        pkg == "com.miui.packageinstaller" ||
        pkg == "com.google.android.permissioncontroller"

private fun isDownloadOrFileManager(pkg: String): Boolean =
    pkg == "com.google.android.apps.nbu.files" ||
        pkg == "com.google.android.documentsui" ||
        pkg == "com.android.documentsui" ||
        pkg == "com.android.providers.downloads" ||
        pkg == "com.android.providers.downloads.ui" ||
        pkg == "com.sec.android.app.myfiles" ||
        pkg.contains("filemanager", ignoreCase = true) ||
        pkg.contains("fileexplorer", ignoreCase = true)

private fun isBrowserPackage(pm: PackageManager, pkg: String): Boolean {
    val knownBrowsers = setOf(
        "com.android.chrome",
        "org.mozilla.firefox",
        "org.mozilla.fenix",
        "com.brave.browser",
        "com.microsoft.emmx",
        "com.opera.browser",
        "com.opera.mini.native",
        "com.opera.gx",
        "com.sec.android.app.sbrowser",
        "com.vivaldi.browser",
        "com.duckduckgo.mobile.android",
        "org.torproject.torbrowser",
        "com.kiwibrowser.browser",
        "mark.via.gp",
    )
    if (pkg in knownBrowsers || pkg.contains("browser", ignoreCase = true)) return true

    return try {
        val browserIntent = Intent(Intent.ACTION_VIEW, "https://github.com".toUri())
        val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(browserIntent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(browserIntent, 0)
        }
        resolved.any { it.activityInfo?.packageName == pkg }
    } catch (_: Exception) {
        false
    }
}
