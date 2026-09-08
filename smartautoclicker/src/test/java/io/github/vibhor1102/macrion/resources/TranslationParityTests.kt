/*
 * Copyright (C) 2026 Vibhor Goel
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.vibhor1102.macrion.resources

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

class TranslationParityTests {

    @Test
    fun `every supported locale contains every translatable resource`() {
        val repositoryRoot = File("..").canonicalFile
        val resourceDirectories = repositoryRoot.walkTopDown()
            .onEnter { it.name !in EXCLUDED_DIRECTORIES }
            .filter { directory ->
                directory.isDirectory && directory.name == "res" &&
                        directory.parentFile?.name == "main" && directory.parentFile?.parentFile?.name == "src"
            }
            .toList()

        assertTrue("No Android resource directories found", resourceDirectories.isNotEmpty())

        val supportedLocales = resourceDirectories
            .flatMap { resourcesDirectory ->
                resourcesDirectory.listFiles()
                    ?.filter { directory: File ->
                        directory.isDirectory && directory.name.startsWith("values-") &&
                                localeQualifierRegex.matches(directory.name.removePrefix("values-"))
                    }
                    .orEmpty()
            }
            .map { it.name.removePrefix("values-") }
            .toSortedSet()

        val failures = resourceDirectories.flatMap { resourcesDirectory ->
            val defaultResources = resourceNames(File(resourcesDirectory, "values"), excludeNonTranslatable = true)
            supportedLocales.mapNotNull { locale ->
                val localeDirectory = File(resourcesDirectory, "values-$locale")
                val missing = if (localeDirectory.isDirectory) {
                    defaultResources - resourceNames(localeDirectory, excludeNonTranslatable = false)
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

    private fun resourceNames(directory: File, excludeNonTranslatable: Boolean): Set<ResourceName> =
        directory.listFiles { file -> file.isFile && file.extension == "xml" }
            .orEmpty()
            .flatMap { file ->
                val document = documentBuilderFactory().newDocumentBuilder().parse(file)
                buildList {
                    RESOURCE_TAGS.forEach { tag ->
                        val nodes = document.getElementsByTagName(tag)
                        for (index in 0 until nodes.length) {
                            val element = nodes.item(index) as Element
                            if (!excludeNonTranslatable || element.getAttribute("translatable") != "false") {
                                element.getAttribute("name").takeIf(String::isNotEmpty)?.let { name ->
                                    add(ResourceName(tag, name))
                                }
                            }
                        }
                    }
                }
            }
            .toSet()

    private fun documentBuilderFactory() = DocumentBuilderFactory.newInstance().apply {
        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        setFeature("http://xml.org/sax/features/external-general-entities", false)
        setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        setAttribute("http://javax.xml.XMLConstants/property/accessExternalDTD", "")
        setAttribute("http://javax.xml.XMLConstants/property/accessExternalSchema", "")
    }

    private data class ResourceName(val type: String, val name: String) : Comparable<ResourceName> {
        override fun compareTo(other: ResourceName): Int =
            compareValuesBy(this, other, ResourceName::type, ResourceName::name)

        override fun toString(): String = "$type/$name"
    }

    private companion object {
        val RESOURCE_TAGS = listOf("string", "plurals", "string-array")
        val EXCLUDED_DIRECTORIES = setOf(".agents", ".git", ".gradle", "backend", "build", "node_modules")
        val localeQualifierRegex = Regex("""(?:[a-z]{2,3}(?:-r[A-Z]{2})?|b\\+[A-Za-z]{2,8}(?:\\+[A-Za-z0-9]{1,8})*)""")
    }
}
