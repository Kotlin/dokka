package org.jetbrains.dokka

import java.io.File
import java.net.MalformedURLException
import java.net.URL

@OptIn(ExperimentalStdlibApi::class) // for buildList
fun defaultLinks(config: DokkaConfiguration.DokkaSourceSet): MutableList<DokkaConfiguration.ExternalDocumentationLink> =
    buildList<DokkaConfiguration.ExternalDocumentationLink> {
        if (!config.noJdkLink) {
            add(DokkaConfiguration.ExternalDocumentationLink.jdk(config.jdkVersion))
        }

        if (!config.noStdlibLink) {
            add(DokkaConfiguration.ExternalDocumentationLink.kotlinStdlib())
        }
    }.toMutableList()


fun parseLinks(links: List<String>): List<DokkaConfiguration.ExternalDocumentationLink> {
    val (parsedLinks, parsedOfflineLinks) = links
        .map { it.split("^").map { it.trim() }.filter { it.isNotBlank() } }
        .filter { it.isNotEmpty() }
        .partition { it.size == 1 }

    return parsedLinks.map { (root) -> ExternalDocumentationLink(root) } +
            parsedOfflineLinks.map { (root, packageList) ->
                val rootUrl = URL(root)
                val packageListUrl =
                    try {
                        URL(packageList)
                    } catch (ex: MalformedURLException) {
                        File(packageList).toURI().toURL()
                    }
                ExternalDocumentationLink(rootUrl, packageListUrl)
            }
}
