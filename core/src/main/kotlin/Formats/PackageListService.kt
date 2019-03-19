package org.jetbrains.dokka

import com.google.inject.Inject


interface PackageListService {
    fun formatPackageList(module: DocumentationModule): String
}

class DefaultPackageListService @Inject constructor(
        val generator: NodeLocationAwareGenerator,
        val formatService: FormatService
) : PackageListService {

    override fun formatPackageList(module: DocumentationModule): String {
        val packages = mutableSetOf<String>()
        val nonStandardLocations = mutableMapOf<String, String>()

        fun visit(node: DocumentationNode, relocated: Boolean = false) {
            val nodeKind = node.kind

            when (nodeKind) {
                NodeKind.Package -> {
                    packages.add(node.qualifiedName())
                    node.members.forEach { visit(it) }
                }
                NodeKind.Signature -> {
                    if (relocated)
                        nonStandardLocations[node.name] = generator.relativePathToLocation(module, node.owner!!)
                }
                NodeKind.ExternalClass -> {
                    node.members.forEach { visit(it, relocated = true) }
                }
                NodeKind.GroupNode -> {
                    if (node.members.isNotEmpty()) {
                        // Only nodes only has single file is need to be relocated
                        // TypeAliases for example
                        node.origins
                                .filter { it.members.isEmpty() }
                                .forEach { visit(it, relocated = true) }
                    }
                }
                else -> {
                    if (nodeKind in NodeKind.classLike || nodeKind in NodeKind.memberLike) {
                        node.details(NodeKind.Signature).forEach { visit(it, relocated) }
                        node.members.forEach { visit(it, relocated) }
                    }
                }
            }
        }

        module.members.forEach { visit(it) }

        return buildString {
            appendln("\$dokka.linkExtension:${formatService.linkExtension}")

            nonStandardLocations.map { (signature, location) -> "\$dokka.location:$signature\u001f$location" }
                    .sorted().joinTo(this, separator = "\n", postfix = "\n")

            packages.sorted().joinTo(this, separator = "\n", postfix = "\n")
        }

    }

}

