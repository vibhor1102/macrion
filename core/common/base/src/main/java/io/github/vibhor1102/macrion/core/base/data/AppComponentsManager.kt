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
package io.github.vibhor1102.macrion.core.base.data

import android.content.ComponentName
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton


interface AppComponentsProvider {
    val originalAppId: String

    val macrionServiceComponentName: ComponentName
    val scenarioActivityComponentName: ComponentName
    val tutorialActivityComponentName: ComponentName
}


@Singleton
class AppComponentsManager @Inject constructor(
    @ApplicationContext private val context: Context,
) : AppComponentsProvider {

    private lateinit var _originalAppId: String
    override val originalAppId: String
        get() = _originalAppId

    private lateinit var _macrionServiceComponentName: ComponentName
    override val macrionServiceComponentName: ComponentName
        get() = _macrionServiceComponentName

    private lateinit var _scenarioActivityComponentName: ComponentName
    override val scenarioActivityComponentName: ComponentName
        get() = _scenarioActivityComponentName

    override val tutorialActivityComponentName: ComponentName
        get() = ComponentName(
            context.packageName,
            "io.github.vibhor1102.macrion.feature.tutorial.ui.TutorialActivity",
        )

    fun registerOriginalAppId(appId: String) {
        _originalAppId = appId
    }

    fun registerScenarioActivity(componentName: ComponentName) {
        _scenarioActivityComponentName = componentName
    }

    fun registerSmartAutoClickerService(componentName: ComponentName) {
        _macrionServiceComponentName = componentName
    }
}