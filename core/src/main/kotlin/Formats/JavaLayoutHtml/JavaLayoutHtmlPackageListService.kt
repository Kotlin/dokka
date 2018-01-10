package org.jetbrains.dokka.Formats

import org.jetbrains.dokka.DocumentationModule
import org.jetbrains.dokka.ExternalDocumentationLinkResolver.Companion.DOKKA_PARAM_PREFIX
import org.jetbrains.dokka.NodeKind
import org.jetbrains.dokka.PackageListService

class JavaLayoutHtmlPackageListService : PackageListService {

    private fun StringBuilder.appendParam(name: String, value: String) {
        append(DOKKA_PARAM_PREFIX)
        append(name)
        append(":")
        appendln(value)
    }

    override fun formatPackageList(module: DocumentationModule): String {
        val packages = module.members(NodeKind.Package).map { it.name }

        return buildString {
            appendParam("format", "java-layout-html")
            for (p in packages) {
                appendln(p)
            }
        }
    }

}