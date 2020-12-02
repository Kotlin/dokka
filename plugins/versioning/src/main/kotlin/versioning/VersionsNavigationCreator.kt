package org.jetbrains.dokka.versioning

import kotlinx.html.a
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.i
import kotlinx.html.stream.appendHTML
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import java.nio.file.Path

interface VersionsNavigationCreator : (Path) -> String, () -> String

class HtmlVersionsNavigationCreator(val context: DokkaContext) : VersionsNavigationCreator {

    private val versioningHandler by lazy { context.plugin<VersioningPlugin>().querySingle { versioningHandler } }

    override fun invoke(): String =
        versioningHandler.currentVersion()?.let { this(it) }.orEmpty()

    override fun invoke(output: Path): String {
        val position = output.takeIf { it.toFile().isDirectory } ?: output.parent
        return versioningHandler.getVersions().takeIf { it.isNotEmpty() }?.let { versions ->
            StringBuilder().appendHTML().div(classes = "versions-dropdown") {
                button(classes = "versions-dropdown-button") {
                    versions.entries.first { it.value == output }.let {
                        text(it.key)
                    }
                    i(classes = "fa fa-caret-down")
                }
                div(classes = "versions-dropdown-data") {
                    versions.forEach { (version, path) ->
                        a(href = position.relativize(path).toString()) {
                            text(version)
                        }
                    }
                }
            }.toString()
        }.orEmpty()
    }
}