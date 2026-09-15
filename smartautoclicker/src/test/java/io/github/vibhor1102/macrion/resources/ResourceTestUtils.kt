/*
 * Copyright (C) 2026 Vibhor Goel
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.vibhor1102.macrion.resources

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Document
import org.w3c.dom.Element

internal object ResourceTestUtils {

    val RESOURCE_TAGS = listOf("string", "plurals", "string-array")
    val EXCLUDED_DIRECTORIES = setOf(".agents", ".git", ".gradle", "backend", "build", "node_modules")
    val LOCALE_QUALIFIER_REGEX = Regex("""(?:[a-z]{2,3}(?:-r[A-Z]{2})?|b\+[A-Za-z]{2,8}(?:\+[A-Za-z0-9]{1,8})*)""")

    fun findRepositoryRoot(): File {
        var current: File? = File(".").canonicalFile
        while (current != null && !File(current, "settings.gradle.kts").exists()) {
            current = current.parentFile
        }
        return current ?: File("..").canonicalFile
    }

    fun findResourceDirectories(repositoryRoot: File): List<File> =
        repositoryRoot.walkTopDown()
            .onEnter { it.name !in EXCLUDED_DIRECTORIES }
            .filter { directory ->
                directory.isDirectory && directory.name == "res" &&
                        directory.parentFile?.name == "main" && directory.parentFile?.parentFile?.name == "src"
            }
            .toList()

    fun findSupportedLocales(resourceDirectories: List<File>): Set<String> =
        resourceDirectories
            .flatMap { resourcesDirectory ->
                resourcesDirectory.listFiles()
                    ?.filter { directory: File ->
                        directory.isDirectory && directory.name.startsWith("values-") &&
                                LOCALE_QUALIFIER_REGEX.matches(directory.name.removePrefix("values-"))
                    }
                    .orEmpty()
            }
            .map { it.name.removePrefix("values-") }
            .toSortedSet()

    fun parseXml(file: File): Document =
        documentBuilderFactory.newDocumentBuilder().parse(file)


    fun resourceNames(directory: File, excludeNonTranslatable: Boolean): Set<ResourceName> =
        directory.listFiles { file -> file.isFile && file.extension == "xml" }
            .orEmpty()
            .flatMap { file ->
                val document = parseXml(file)
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

    private val documentBuilderFactory: DocumentBuilderFactory by lazy {
        DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            setAttribute("http://javax.xml.XMLConstants/property/accessExternalDTD", "")
            setAttribute("http://javax.xml.XMLConstants/property/accessExternalSchema", "")
        }
    }
}

internal data class ResourceName(val type: String, val name: String) : Comparable<ResourceName> {
    override fun compareTo(other: ResourceName): Int =
        compareValuesBy(this, other, ResourceName::type, ResourceName::name)

    override fun toString(): String = "$type/$name"
}
