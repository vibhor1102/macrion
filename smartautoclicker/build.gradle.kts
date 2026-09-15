/*
 * Copyright (C) 2025 Kevin Buzeau
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

import com.buzbuz.gradle.convention.model.MacrionBuildType
import com.buzbuz.gradle.convention.model.MacrionFlavour
import com.buzbuz.gradle.convention.extensions.isBuildForVariant
import com.buzbuz.gradle.obfuscation.getExtraActualApplicationId
import org.gradle.api.GradleException

plugins {
    alias(libs.plugins.buzbuz.androidApplication)
    alias(libs.plugins.buzbuz.androidUnitTest)
    alias(libs.plugins.buzbuz.flavour)
    alias(libs.plugins.buzbuz.obfuscation)
    alias(libs.plugins.buzbuz.buildParameters)
    alias(libs.plugins.buzbuz.hilt)
    alias(libs.plugins.jetbrainsKotlinCompose)
}

val supportedAbis = listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
val isRelease = project.isBuildForVariant(MacrionFlavour.F_DROID, MacrionBuildType.RELEASE)
val macrionAbiProperty = providers.gradleProperty("macrionAbi").orNull?.trim()
    ?: providers.gradleProperty("macrionDebugAbi").orNull?.trim()
val targetAbiFilter = when {
    macrionAbiProperty == null -> if (isRelease) supportedAbis else listOf("arm64-v8a")
    macrionAbiProperty.equals("all", ignoreCase = true) -> supportedAbis
    macrionAbiProperty in supportedAbis -> listOf(macrionAbiProperty)
    else -> throw GradleException(
        "Unsupported macrionAbi '$macrionAbiProperty'. " +
                "Use one of ${supportedAbis.joinToString()}, or 'all'.",
    )
}

obfuscationConfig {
    obfuscatedApplication {
        create("io.github.vibhor1102.macrion.application.SmartAutoClickerApplication")
    }
    obfuscatedComponents {
        create("io.github.vibhor1102.macrion.scenarios.ScenarioActivity")
        create("io.github.vibhor1102.macrion.SmartAutoClickerService")
    }

    setup(
        applicationId = "io.github.vibhor1102.macrion",
        appNameResId = "@string/app_name",
        shouldRandomize = buildParameters.randomizeAppId.typedValue &&
                project.isBuildForVariant(MacrionFlavour.F_DROID),
    )
}

android {
    namespace = "io.github.vibhor1102.macrion"

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    androidResources {
        noCompress += listOf("bin", "param")
    }

    packaging {
        resources {
            excludes += listOf(
                "META-INF/version-control-info.textproto",
            )
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    defaultConfig {
        applicationId = getExtraActualApplicationId()

        versionCode = 9
        versionName = "0.4.4"
    }

    if (project.isBuildForVariant(MacrionFlavour.F_DROID, MacrionBuildType.DEBUG)) {
        buildTypes {
            debug {
                applicationIdSuffix = ".debug"
            }
        }
    }

    // Generate per-ABI APKs for fDroid (reduces download size; full APK also produced)
    if (project.isBuildForVariant(MacrionFlavour.F_DROID)) {
        splits {
            abi {
                isEnable = true
                reset()

                val shouldBuildUniversal = isRelease && (macrionAbiProperty == null || macrionAbiProperty.equals("all", ignoreCase = true))
                include(*targetAbiFilter.toTypedArray())
                isUniversalApk = shouldBuildUniversal
            }
        }
    }

    signingConfigs {
        create(MacrionBuildType.RELEASE.buildTypeName) {
            storeFile = file("./smartautoclicker.jks")
            storePassword = buildParameters.signingStorePassword.typedValue
            keyAlias = buildParameters.signingKeyAlias.typedValue
            keyPassword = buildParameters.signingKeyPassword.typedValue
        }
    }

}

// Assign unique versionCodes per ABI for fDroid multi-APK publishing
if (project.isBuildForVariant(MacrionFlavour.F_DROID)) {
    androidComponents.onVariants { variant ->
        variant.outputs.forEach { output ->
            val abiFilter = output.filters
                .find { it.filterType == com.android.build.api.variant.FilterConfiguration.FilterType.ABI }
                ?.identifier
            val abiVersionCode = when (abiFilter) {
                "armeabi-v7a" -> 1
                "arm64-v8a"   -> 2
                "x86"         -> 3
                "x86_64"      -> 4
                else          -> 0  // universal
            }
            val baseVersionCode = output.versionCode.get()
            output.versionCode.set(baseVersionCode * 10_000 + abiVersionCode)
        }
    }
}

// Apply signature convention after declaring the signingConfigs
apply(plugin = libs.plugins.buzbuz.androidSigning.get().pluginId)

dependencies {
    implementation(libs.acra.core) {
        // AutoService is an annotation processor accidentally exposed by ACRA at runtime.
        // It is not needed on Android and makes R8 look for unavailable javax.annotation APIs.
        exclude(group = "com.google.auto.service", module = "auto-service")
    }
    val composeBom = platform(libs.androidx.compose.bom)

    implementation(libs.kotlinx.coroutines.core)

    implementation(composeBom)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.datastore)

    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.common.java8)

    implementation(project(":core:common:accessibility"))
    implementation(project(":core:common:actions"))
    implementation(project(":core:common:base"))
    implementation(project(":core:common:bitmaps"))
    implementation(project(":core:common:display"))
    implementation(project(":core:common:navigation"))
    implementation(project(":core:common:overlays"))
    implementation(project(":core:common:permissions"))
    implementation(project(":core:common:quality"))
    implementation(project(":core:common:settings"))
    implementation(project(":core:common:tutorial"))
    implementation(project(":core:common:ui"))
    implementation(project(":core:dumb"))
    implementation(project(":core:smart:debugging"))
    implementation(project(":core:smart:detection"))
    implementation(project(":core:smart:domain"))
    implementation(project(":core:smart:processing"))

    implementation(project(":feature:backup"))
    implementation(project(":feature:notifications"))
    implementation(project(":feature:external-launch"))
    implementation(project(":feature:revenue"))
    implementation(project(":feature:review"))
    implementation(project(":feature:smart-config"))
    implementation(project(":feature:smart-debugging"))
    implementation(project(":feature:dumb-config"))
    implementation(project(":feature:tutorial"))
}
