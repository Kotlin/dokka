/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.versioning

import kotlinx.html.a
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.i
import kotlinx.html.li
import kotlinx.html.span
import kotlinx.html.stream.appendHTML
import kotlinx.html.ul
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.configuration
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.utilities.urlEncoded
import java.io.File

public fun interface VersionsNavigationCreator {
    public operator fun invoke(output: File): String
}

public class HtmlVersionsNavigationCreator(
    private val context: DokkaContext
) : VersionsNavigationCreator {

    private val versioningStorage by lazy { context.plugin<VersioningPlugin>().querySingle { versioningStorage } }

    private val versionsOrdering by lazy { context.plugin<VersioningPlugin>().querySingle { versionsOrdering } }

    private val isOnlyOnRootPage =
        configuration<VersioningPlugin, VersioningConfiguration>(context)?.renderVersionsNavigationOnAllPages == false

    private val versions: Map<VersionId, File> by lazy {
        versioningStorage.previousVersions.map { (k, v) -> k to v.dst }.toMap() +
                (versioningStorage.currentVersion.name to versioningStorage.currentVersion.dir)
    }

    override fun invoke(output: File): String {
        if (versions.size == 1) {
            return versioningStorage.currentVersion.name
        }
        val position = output.takeIf { it.isDirectory } ?: output.parentFile
        if (isOnlyOnRootPage) {
            getActiveVersion(position)?.takeIf {
                it.value == versioningStorage.currentVersion.dir
                        && it.value != position
            }?.also { return@invoke it.key }
        }
        return versions
            .let { versions -> versionsOrdering.order(versions.keys.toList()).map { it to versions[it] } }
            .takeIf { it.isNotEmpty() }
            ?.let { orderedVersions ->
                StringBuilder().appendHTML().div(classes = "dropdown versions-dropdown") {
                    attributes["data-role"] = "dropdown"
                    val activeVersion = getActiveVersion(position)
                    val relativePosition: String =
                        activeVersion?.value?.let { output.toRelativeString(it) } ?: "index.html"
                    button(classes = "button button_dropdown") {
                        attributes["role"] = "combobox"
                        attributes["data-role"] = "dropdown-toggle"
                        attributes["aria-controls"] = "versions-listbox"
                        attributes["aria-haspopup"] = "listbox"
                        attributes["aria-expanded"] = "false"
                        attributes["aria-label"] = "Select version"
                        activeVersion?.key?.let { text(it) }
                    }
                    ul(classes = "dropdown--list") {
                        attributes["role"] = "listbox"
                        attributes["data-role"] = "dropdown-listbox"
                        attributes["aria-label"] = "Versions"
                        attributes["id"] = "versions-listbox"
                        div(classes = "dropdown--header") {
                            span { text("Select version") }
                            button(classes = "button") {
                                attributes["data-role"] = "dropdown-toggle"
                                attributes["aria-label"] = "Close versions selection"
                                i(classes = "ui-kit-icon ui-kit-icon_cross") {}
                            }
                        }
                        orderedVersions.forEach { (version, path) ->
                            li {
                                if (version == activeVersion?.key) {
                                    a(classes = "dropdown--option dropdown--option-link", href = output.name) {
                                        attributes["role"] = "option"
                                        text(version)
                                    }
                                } else {
                                    val isExistsFile =
                                        if (version == versioningStorage.currentVersion.name)
                                            path?.resolve(relativePosition)?.exists() == true
                                        else
                                            versioningStorage.previousVersions[version]?.src?.resolve(relativePosition)
                                                ?.exists() == true

                                    val absolutePath =
                                        if (isExistsFile)
                                            path?.resolve(relativePosition)
                                        else
                                            versioningStorage.currentVersion.dir.resolve("not-found-version.html")

                                    a(
                                        classes = "dropdown--option dropdown--option-link",
                                        href = absolutePath?.toRelativeString(position) +
                                                if (!isExistsFile) "?v=" + version.urlEncoded() else ""
                                    ) {
                                        attributes["role"] = "option"
                                        text(version)
                                    }
                                }
                            }
                        }
                    }
                    div(classes = "dropdown--overlay") {}
                }.toString()
            }.orEmpty()
    }

    private fun getActiveVersion(position: File) =
        versions.minByOrNull { (_, versionLocation) ->
            versionLocation.let { position.toRelativeString(it).length }
        }
}
