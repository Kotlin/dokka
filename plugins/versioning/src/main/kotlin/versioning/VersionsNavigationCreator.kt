package org.jetbrains.dokka.versioning

import com.jetbrains.rd.util.first
import kotlinx.html.a
import kotlinx.html.div
import kotlinx.html.stream.appendHTML
import org.jetbrains.dokka.base.renderers.html.strike
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.configuration
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import java.io.File

interface VersionsNavigationCreator {
    operator fun invoke(location: String): String
    operator fun invoke(output: File): String
}

class HtmlVersionsNavigationCreator(private val context: DokkaContext) : VersionsNavigationCreator {

    private val versioningHandler by lazy { context.plugin<VersioningPlugin>().querySingle { versioningHandler } }

    private val versionsOrdering by lazy { context.plugin<VersioningPlugin>().querySingle { versionsOrdering } }

    private val isOnlyOnRootPage =
        configuration<VersioningPlugin, VersioningConfiguration>(context)?.isOnlyOnRootPage != false

    override fun invoke(location: String): String =
        versioningHandler.currentVersion()?.let { invoke(it.resolve(location)) }.orEmpty()

    override fun invoke(output: File): String {
        if (versioningHandler.versions.size == 1) {
            return versioningHandler.versions.first().key
        }
        val position = output.takeIf { it.isDirectory } ?: output.parentFile
        if(isOnlyOnRootPage ) {
            fromCurrentVersion(position)?.takeIf { position != it.value }?.also { return@invoke it.key }
        }
        return versioningHandler.versions
            .let { versions -> versionsOrdering.order(versions.keys.toList()).map { it to versions[it] } }
            .takeIf { it.isNotEmpty() }
            ?.let { versions ->
                StringBuilder().appendHTML().div(classes = "versions-dropdown") {
                    var relativePosition: String? = null
                    var activeVersion = ""
                    div(classes = "versions-dropdown-button") {
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

    private fun fromCurrentVersion(position: File) =
        versioningHandler.versions.minByOrNull { (_, versionLocation) ->
            versionLocation.let { position.toRelativeString(it).length }
        }.takeIf { it?.value == versioningHandler.currentVersion() }
}