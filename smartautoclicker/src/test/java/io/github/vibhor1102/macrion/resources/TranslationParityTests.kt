/*
 * Copyright (C) 2026 Vibhor Goel
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.vibhor1102.macrion.resources

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class TranslationParityTests {

    @Test
    fun `every supported locale contains every translatable resource`() {
        val repositoryRoot = ResourceTestUtils.findRepositoryRoot()
        val resourceDirectories = ResourceTestUtils.findResourceDirectories(repositoryRoot)
        assertTrue("No Android resource directories found", resourceDirectories.isNotEmpty())

        val supportedLocales = ResourceTestUtils.findSupportedLocales(resourceDirectories)

        val failures = resourceDirectories.flatMap { resourcesDirectory ->
            val defaultResources = ResourceTestUtils.resourceNames(File(resourcesDirectory, "values"), excludeNonTranslatable = true)
            supportedLocales.mapNotNull { locale ->
                val localeDirectory = File(resourcesDirectory, "values-$locale")
                val missing = if (localeDirectory.isDirectory) {
                    defaultResources - ResourceTestUtils.resourceNames(localeDirectory, excludeNonTranslatable = false)
                } else {
                    defaultResources
                }
                missing.takeIf { it.isNotEmpty() }?.let {
                    "${resourcesDirectory.relativeTo(repositoryRoot)}/values-$locale: ${it.sorted().joinToString()}"
                }
            }
        }

        assertTrue(
            "Missing translations:\n${failures.joinToString("\n")}",
            failures.isEmpty(),
        )
    }

    @Test
    fun `no localized file contains orphaned resources not in default values`() {
        val repositoryRoot = ResourceTestUtils.findRepositoryRoot()
        val resourceDirectories = ResourceTestUtils.findResourceDirectories(repositoryRoot)
        assertTrue("No Android resource directories found", resourceDirectories.isNotEmpty())

        val failures = resourceDirectories.flatMap { resourcesDirectory ->
            val defaultValuesDir = File(resourcesDirectory, "values")
            val defaultResources = if (defaultValuesDir.isDirectory) {
                ResourceTestUtils.resourceNames(defaultValuesDir, excludeNonTranslatable = false)
            } else {
                emptySet()
            }

            resourcesDirectory.listFiles { dir ->
                dir.isDirectory && dir.name.startsWith("values-") &&
                        ResourceTestUtils.LOCALE_QUALIFIER_REGEX.matches(dir.name.removePrefix("values-"))
            }.orEmpty().mapNotNull { localeDirectory ->
                val localeResources = ResourceTestUtils.resourceNames(localeDirectory, excludeNonTranslatable = false)
                val orphaned = localeResources - defaultResources
                orphaned.takeIf { it.isNotEmpty() }?.let {
                    "${localeDirectory.relativeTo(repositoryRoot)}: ${it.sorted().joinToString()}"
                }
            }
        }

        assertTrue(
            "Orphaned translations not present in default values:\n${failures.joinToString("\n")}",
            failures.isEmpty(),
        )
    }
}
