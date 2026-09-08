/*
 * Copyright (C) 2024 Kevin Buzeau
 * Copyright (C) 2026 Vibhor Goel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.vibhor1102.macrion.settings

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.os.Build
import io.github.vibhor1102.macrion.crash.CrashReportsActivity
import io.github.vibhor1102.macrion.core.base.crash.CrashDiagnostics
import io.github.vibhor1102.macrion.BuildConfig
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        CrashDiagnostics.record(CrashDiagnostics.Event.SETTINGS_OPENED)
        setContent {
            SettingsRoute(
                viewModel = viewModel,
                onNavigateBack = ::finish,
                onShowPrivacySettings = { viewModel.showPrivacySettings(this) },
                onShowPurchase = { viewModel.showPurchaseActivity(this) },
                onShowTroubleshooting = { viewModel.showTroubleshootingDialog(this) },
                onShowCrashReports = { startActivity(Intent(this, CrashReportsActivity::class.java)) },
                onOpenGithub = { openUrl(GITHUB_URL) },
                onJoinDiscord = { openUrl(DISCORD_URL) },
                onReportBug = { openUrl(createBugReportUrl()) },
            )
        }
    }

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun createBugReportUrl(): String = Uri.parse(BUG_REPORT_URL).buildUpon()
        .appendQueryParameter("template", BUG_REPORT_TEMPLATE)
        .appendQueryParameter("app-version", BuildConfig.VERSION_NAME)
        .appendQueryParameter("device-type", "${Build.MANUFACTURER} ${Build.MODEL}".trim())
        .appendQueryParameter("android-version", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        .build()
        .toString()

    private companion object {
        const val GITHUB_URL = "https://github.com/vibhor1102/macrion"
        const val DISCORD_URL = "https://discord.gg/g3RpJxtWFZ"
        const val BUG_REPORT_URL = "https://github.com/vibhor1102/macrion/issues/new"
        const val BUG_REPORT_TEMPLATE = "bug_report.yml"
    }
}
