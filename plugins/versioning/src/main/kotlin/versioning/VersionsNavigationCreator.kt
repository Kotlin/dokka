package org.jetbrains.dokka.versioning

import kotlinx.html.a
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.i
import kotlinx.html.stream.appendHTML
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import java.io.File
import java.nio.file.Files.isDirectory
import java.nio.file.Path

interface VersionsNavigationCreator {
    operator fun invoke(): String
    operator fun invoke(output: File): String
}

class HtmlVersionsNavigationCreator(val context: DokkaContext) : VersionsNavigationCreator {

    private val versioningHandler by lazy { context.plugin<VersioningPlugin>().querySingle { versioningHandler } }

    private val versionsOrdering by lazy { context.plugin<VersioningPlugin>().querySingle { versionsOrdering } }

    override fun invoke(): String =
        versioningHandler.currentVersion()?.let { invoke(it) }.orEmpty()

    override fun invoke(output: File): String {
        val position = output.takeIf { it.isDirectory } ?: output.parentFile
        return versioningHandler.getVersions()
            .let { versions -> versionsOrdering.order(versions.keys.toList()).map { it to versions[it] } }
            .takeIf { it.isNotEmpty() }
            ?.let { versions ->
                StringBuilder().appendHTML().div(classes = "versions-dropdown") {
                    button(classes = "versions-dropdown-button") {
                        versions.first { (_, versionLocation) -> versionLocation?.absolutePath == position.absolutePath }
                            .let { (version, _) ->
                                text(version)
                            }
                        i(classes = "fa fa-caret-down")
                    }
                    div(classes = "versions-dropdown-data") {
                        versions.forEach { (version, path) ->
                            a(href = path?.resolve("index.html")?.toRelativeString(position)) {
                                text(version)
                            }
                        }
                    }
                }.toString()
            }.orEmpty()
    }
}