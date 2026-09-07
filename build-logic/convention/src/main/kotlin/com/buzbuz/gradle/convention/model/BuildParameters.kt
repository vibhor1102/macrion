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
package com.buzbuz.gradle.convention.model

import org.gradle.api.Project
import org.gradle.api.provider.Property

abstract class BuildParameters {

    abstract val rootProject: Property<Project>

    /** Tells if the application ID and components should be randomized at build time. */
    val randomizeAppId: BuildParameter<Boolean> by lazy {
        BuildParameter(
            rootProject = rootProject.get(),
            name = "randomizeAppId",
            defaultValue = false,
        )
    }

    /** Release signing configuration store password. */
    val signingStorePassword: BuildParameter<String> by lazy {
        BuildParameter(
            rootProject = rootProject.get(),
            name = "signingStorePassword",
            defaultValue = "",
        )
    }

    /** Release signing configuration key alias. */
    val signingKeyAlias: BuildParameter<String> by lazy {
        BuildParameter(
            rootProject = rootProject.get(),
            name = "signingKeyAlias",
            defaultValue = "",
        )
    }

    /** Release signing configuration key password. */
    val signingKeyPassword: BuildParameter<String> by lazy {
        BuildParameter(
            rootProject = rootProject.get(),
            name = "signingKeyPassword",
            defaultValue = "",
        )
    }

}
