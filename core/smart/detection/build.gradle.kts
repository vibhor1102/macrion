@file:Suppress("UnstableApiUsage")

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

import com.buzbuz.gradle.convention.extensions.fDroid
import org.gradle.api.GradleException

plugins {
    alias(libs.plugins.buzbuz.androidLibrary)
    alias(libs.plugins.buzbuz.androidLocalTest)
    alias(libs.plugins.buzbuz.flavour)
    alias(libs.plugins.buzbuz.sourceDownload)
}

val supportedAbis = listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
val isReleaseBuild = gradle.startParameter.taskNames.any { it.contains("Release", ignoreCase = true) }
val macrionAbiProperty = providers.gradleProperty("macrionAbi").orNull?.trim()
    ?: providers.gradleProperty("macrionDebugAbi").orNull?.trim()
val targetAbiFilter = when {
    macrionAbiProperty == null -> if (isReleaseBuild) emptyList() else listOf("arm64-v8a")
    macrionAbiProperty.equals("all", ignoreCase = true) -> emptyList()
    macrionAbiProperty in supportedAbis -> listOf(macrionAbiProperty)
    else -> throw GradleException(
        "Unsupported macrionAbi '$macrionAbiProperty'. " +
                "Use one of ${supportedAbis.joinToString()}, or 'all'.",
    )
}

sourceDownload {
    projects {
        register("openCv") {
            projectAccount = "opencv"
            projectName = "opencv"
            projectVersion = libs.versions.openCv.get()

            unzipPath = File("src/release/opencv")
            requiredForTask = "configureCMakeRelease"
        }

        register("ncnn") {
            projectAccount = "Tencent"
            projectName = "ncnn"
            projectVersion = libs.versions.ncnn.get()

            unzipPath = File("src/release/ncnn")
            requiredForTask = "configureCMakeRelease"
        }
    }
}

android {
    namespace = "io.github.vibhor1102.macrion.core.detection"

    androidResources {
        noCompress += listOf("bin", "param")
    }

    defaultConfig {
        if (targetAbiFilter.isNotEmpty()) {
            ndk {
                abiFilters.addAll(targetAbiFilter)
            }
        }

        externalNativeBuild {
            cmake {
                if (targetAbiFilter.isNotEmpty()) {
                    abiFilters.addAll(targetAbiFilter)
                }
                if (System.getenv("USE_CCACHE") == "true") {
                    arguments.addAll(
                        listOf(
                            "-DCMAKE_C_COMPILER_LAUNCHER=ccache",
                            "-DCMAKE_CXX_COMPILER_LAUNCHER=ccache",
                        )
                    )
                }
            }
        }
    }

    buildTypes {
        debug {
            externalNativeBuild {
                cmake {
                    arguments.addAll(
                        listOf("-DCMAKE_BUILD_TYPE=Debug")
                    )
                }
            }
        }

        release {
            ndk {
                debugSymbolLevel = "NONE"
            }
            externalNativeBuild {
                cmake {
                    arguments.addAll(
                        listOf(
                            "-DANDROID_SDK_ROOT=${project.androidComponents.sdkComponents.sdkDirectory}",
                            "-DCMAKE_BUILD_TYPE=Release",
                            "-DCMAKE_C_FLAGS=-ffile-prefix-map=${project.rootDir.absolutePath}=.",
                            "-DCMAKE_CXX_FLAGS=-ffile-prefix-map=${project.rootDir.absolutePath}=.",
                            "-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON",
                            "-DOPENCV_ENABLE_NONFREE=OFF",
                            "-DBUILD_opencv_ittnotify=OFF",
                            "-DBUILD_ITT=OFF",
                            "-DCV_DISABLE_OPTIMIZATION=ON",
                            "-DWITH_CUDA=OFF",
                            "-DWITH_OPENCL=OFF",
                            "-DWITH_OPENCLAMDFFT=OFF",
                            "-DWITH_OPENCLAMDBLAS=OFF",
                            "-DWITH_VA_INTEL=OFF",
                            "-DENABLE_SSE=OFF",
                            "-DENABLE_SSE2=OFF",
                            "-DBUILD_TESTING=OFF",
                            "-DBUILD_PERF_TESTS=OFF",
                            "-DBUILD_TESTS=OFF",
                            "-DBUILD_EXAMPLES=OFF",
                            "-DBUILD_DOCS=OFF",
                            "-DBUILD_opencv_apps=OFF",
                            "-DWITH_1394=OFF",
                            "-DWITH_ARITH_DEC=OFF",
                            "-DWITH_ARITH_ENC=OFF",
                            "-DWITH_CUBLAS=OFF",
                            "-DWITH_CUFFT=OFF",
                            "-DWITH_FFMPEG=OFF",
                            "-DWITH_GDAL=OFF",
                            "-DWITH_GSTREAMER=OFF",
                            "-DWITH_GTK=OFF",
                            "-DWITH_HALIDE=OFF",
                            "-DWITH_JASPER=OFF",
                            "-DWITH_NVCUVID=OFF",
                            "-DWITH_OPENEXR=OFF",
                            "-DWITH_PROTOBUF=OFF",
                            "-DWITH_PTHREADS_PF=OFF",
                            "-DWITH_QUIRC=OFF",
                            "-DWITH_V4L=OFF",
                            "-DWITH_WEBP=OFF",
                            "-DBUILD_LIST=core,imgproc",
                            "-DBUILD_JAVA=OFF",
                            "-DBUILD_ANDROID_EXAMPLES=OFF",
                            "-DBUILD_ANDROID_PROJECTS=OFF",
                            "-DBUILD_SHARED_LIBS=ON",
                            "-DNCNN_SHARED_LIB=ON",
                            "-DNCNN_BUILD_TOOLS=OFF",
                            "-DNCNN_BUILD_EXAMPLES=OFF",
                            "-DNCNN_BUILD_BENCHMARK=OFF",
                            "-DNCNN_BUILD_TESTS=OFF",
                            "-DNCNN_VULKAN=OFF",
                            "-DNCNN_OPENMP=OFF",
                            "-DNCNN_RUNTIME_CPU=ON",
                            "-DNCNN_DISABLE_RTTI=OFF",
                            "-DNCNN_DISABLE_EXCEPTION=OFF",
                            "-DNCNN_AVX=OFF",
                            "-DNCNN_AVX2=OFF",
                            "-DNCNN_AVX512=OFF",
                            "-DNCNN_FMA=OFF",
                            "-DNCNN_BF16=OFF",
                            "-DNCNN_FP16=OFF",
                            "-DNCNN_INT8=OFF",
                        )
                    )
                }
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = File("src/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    productFlavors {
        fDroid {
            externalNativeBuild.cmake.arguments.addAll(
                listOf("-DWITH_BUILD_ID=OFF")
            )
        }
    }
}

dependencies {
    implementation(libs.androidx.annotation)
    implementation(project(":core:common:base"))
}
