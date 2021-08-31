package org.jetbrains.dokka.versioning

import kotlinx.html.a
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.i
import kotlinx.html.stream.appendHTML
import org.jetbrains.dokka.base.renderers.html.strike
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import java.io.File
import java.nio.file.Files.isDirectory
import java.nio.file.Path

interface VersionsNavigationCreator {
    operator fun invoke(location: String): String
    operator fun invoke(output: File): String
}

class HtmlVersionsNavigationCreator(val context: DokkaContext) : VersionsNavigationCreator {

    private val versioningHandler by lazy { context.plugin<VersioningPlugin>().querySingle { versioningHandler } }

    private val versionsOrdering by lazy { context.plugin<VersioningPlugin>().querySingle { versionsOrdering } }

    override fun invoke(location: String): String =
        versioningHandler.currentVersion()?.let { invoke(it.resolve(location)) }.orEmpty()

    override fun invoke(output: File): String {
        val position = output.takeIf { it.isDirectory } ?: output.parentFile
        return versioningHandler.versions
            .let { versions -> versionsOrdering.order(versions.keys.toList()).map { it to versions[it] } }
            .takeIf { it.isNotEmpty() }
            ?.let { versions ->
                StringBuilder().appendHTML().div(classes = "versions-dropdown") {
                    var relativePosition: String? = null
                    var activeVersion = ""
                    button(classes = "versions-dropdown-button") {
                        versions.minByOrNull { (_, versionLocation) ->
                            versionLocation?.let { position.toRelativeString(it).length } ?: 0xffffff
                        }?.let { (version, versionRoot) ->
                            relativePosition = versionRoot?.let { output.toRelativeString(it) }
                            activeVersion = version
                            text(version)
                        }
                    }
                    div(classes = "versions-dropdown-data") {
                        versions.forEach { (version, path) ->
                            if (version == activeVersion) {
                                a(href = output.name) { text(version) }
                            } else {
                                var exist = false
                                val absolutePath = path?.resolve(relativePosition ?: "index.html")?.takeIf {
                                    it.exists() ||
                                            versioningHandler.previousVersions[version]?.src?.resolve(
                                                relativePosition ?: "index.html"
                                            )?.exists() == true
                                }?.also { exist = true }
                                    ?: path?.resolve("index.html")

                                a(href = absolutePath?.toRelativeString(position)) {
                                    if(exist)
                                        text(version)
                                    else
                                        strike { text(version) }
                                }
                            }
                        }
                    }
                }.toString()
            }.orEmpty()
    }
}