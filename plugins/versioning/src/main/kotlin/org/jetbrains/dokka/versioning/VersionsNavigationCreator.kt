package org.jetbrains.dokka.versioning

import kotlinx.html.a
import kotlinx.html.div
import kotlinx.html.stream.appendHTML
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.configuration
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.utilities.urlEncoded
import java.io.File

fun interface VersionsNavigationCreator {
    operator fun invoke(output: File): String
}

class HtmlVersionsNavigationCreator(private val context: DokkaContext) : VersionsNavigationCreator {

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
                StringBuilder().appendHTML().div(classes = "versions-dropdown") {
                    val activeVersion = getActiveVersion(position)
                    val relativePosition: String  = activeVersion?.value?.let { output.toRelativeString(it) } ?: "index.html"
                    div(classes = "versions-dropdown-button") {
                        activeVersion?.key?.let { text(it) }
                    }
                    div(classes = "versions-dropdown-data") {
                        orderedVersions.forEach { (version, path) ->
                            if (version == activeVersion?.key) {
                                a(href = output.name) { text(version) }
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

                                a(href = absolutePath?.toRelativeString(position) +
                                        if (!isExistsFile) "?v=" + version.urlEncoded() else "") {
                                        text(version)
                                }
                            }
                        }
                    }
                }.toString()
            }.orEmpty()
    }

    private fun getActiveVersion(position: File) =
        versions.minByOrNull { (_, versionLocation) ->
            versionLocation.let { position.toRelativeString(it).length }
        }
}
