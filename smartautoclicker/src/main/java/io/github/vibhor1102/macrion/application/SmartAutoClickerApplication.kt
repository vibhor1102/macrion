/*
 * Copyright (C) 2025 Kevin Buzeau
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
package io.github.vibhor1102.macrion.application

import android.app.Application
import android.content.Context
import io.github.vibhor1102.macrion.crash.initializeLocalCrashReporting
import io.github.vibhor1102.macrion.crash.captureHistoricalNativeCrash
import io.github.vibhor1102.macrion.ComponentConfig
import io.github.vibhor1102.macrion.core.base.data.AppComponentsManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SmartAutoClickerApplication : Application() {

    @Inject lateinit var appComponentsManager: AppComponentsManager

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        initializeLocalCrashReporting()
    }

    override fun onCreate() {
        super.onCreate()

        captureHistoricalNativeCrash()

        val componentConfig = ComponentConfig
        appComponentsManager.apply {
            registerOriginalAppId(componentConfig.ORIGINAL_APP_ID)
            registerSmartAutoClickerService(componentConfig.smartAutoClickerService)
            registerScenarioActivity(componentConfig.scenarioActivity)
        }
    }
}
