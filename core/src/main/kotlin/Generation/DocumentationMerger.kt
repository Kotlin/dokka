package org.jetbrains.dokka.Generation

import org.jetbrains.dokka.*

class DocumentationMerger(
    private val documentationModules: List<DocumentationModule>,
    val logger: DokkaLogger
) {
    private val producedNodeRefGraph: NodeReferenceGraph = NodeReferenceGraph()
    private val signatureMap: Map<DocumentationNode, String>
    private val oldToNewNodeMap: MutableMap<DocumentationNode, DocumentationNode> = mutableMapOf()

    init {
        if (documentationModules.groupBy { it.name }.size > 1) {
            throw IllegalArgumentException("Modules should have similar names: ${documentationModules.joinToString(", ") {it.name}}")
        }

        signatureMap = documentationModules
            .flatMap { it.nodeRefGraph.nodeMapView.entries }
            .associate { (k, v) -> v to k }


        documentationModules.map { it.nodeRefGraph }
            .flatMap { it.references }
            .forEach { producedNodeRefGraph.addReference(it) }
    }

    private fun mergePackageReferences(
        from: DocumentationNode,
        packages: List<DocumentationReference>
    ): List<DocumentationReference> {
        val packagesByName = packages
            .map { it.to }
            .groupBy { it.name }

        val resultReferences = mutableListOf<DocumentationReference>()
        for ((name, listOfPackages) in packagesByName) {
            try {
                val producedPackage = mergePackagesWithEqualNames(name, from, listOfPackages)
                updatePendingReferences()

                resultReferences.add(
                        DocumentationReference(from, producedPackage, RefKind.Member)
                )
            } catch (t: Throwable) {
                val entries = listOfPackages.joinToString(",") { "references:${it.allReferences().size}" }
                throw Error("Failed to merge package $name from $from with entries $entries. ${t.message}", t)
            }
        }

        return resultReferences
    }

    private fun mergePackagesWithEqualNames(
        name: String,
        from: DocumentationNode,
        packages: List<DocumentationNode>
    ): DocumentationNode {
        val mergedPackage = DocumentationNode(name, Content.Empty, NodeKind.Package)

        for (contentToAppend in packages.map { it.content }.distinct()) {
            mergedPackage.updateContent {
                for (otherChild in contentToAppend.children) {
                    children.add(otherChild)
                }
            }
        }

        for (node in packages) {
            oldToNewNodeMap[node] = mergedPackage
        }

        val references = packages.flatMap { it.allReferences() }
        val mergedReferences = mergeReferences(mergedPackage, references)
        for (ref in mergedReferences) {
            if (ref.kind == RefKind.Owner) {
                continue
            }
            mergedPackage.addReference(ref)
        }

        from.append(mergedPackage, RefKind.Member)

        return mergedPackage
    }

    private fun mergeMemberGroupBy(it: DocumentationNode): String {
        val signature = signatureMap[it]

        if (signature != null) {
            return signature
        }

        logger.error("Failed to find signature for $it in \n${it.allReferences().joinToString { "\n  ${it.kind} ${it.to}" }}")
        return "<ERROR>"
    }

    private fun mergeMemberReferences(
        from: DocumentationNode,
        refs: List<DocumentationReference>
    ): List<DocumentationReference> {
        val membersBySignature: Map<String, List<DocumentationNode>> = refs.map { it.to }
            .groupBy(this::mergeMemberGroupBy)

        val mergedMembers: MutableList<DocumentationReference> = mutableListOf()
        for ((signature, members) in membersBySignature) {
            val newNode = mergeMembersWithEqualSignature(signature, members)

            producedNodeRefGraph.register(signature, newNode)
            updatePendingReferences()
            from.append(newNode, RefKind.Member)

            mergedMembers.add(DocumentationReference(from, newNode, RefKind.Member))
        }

        return mergedMembers
    }

    private fun mergeMembersWithEqualSignature(
        signature: String,
        nodes: List<DocumentationNode>
    ): DocumentationNode {
        require(nodes.isNotEmpty())

        val singleNode = nodes.singleOrNull()
        if (singleNode != null) {
            singleNode.dropReferences { it.kind == RefKind.Owner }
            return singleNode
        }
        val nodeWithMaxPlatforms = nodes.maxBy { it.platforms.size }!!
        val maxPlatforms = nodeWithMaxPlatforms.platforms.toSet()
        val notContained = nodes.filterNot { maxPlatforms.containsAll(it.platforms) }
        val reducedDuplicates = listOf(nodeWithMaxPlatforms) + notContained
        if (!reducedDuplicates.containsAll(nodes)) {
            return mergeMembersWithEqualSignature(signature, reducedDuplicates)
        }

        val groupNode = DocumentationNode(nodes.first().name, Content.Empty, NodeKind.GroupNode)
        groupNode.appendTextNode(signature, NodeKind.Signature, RefKind.Detail)

        for (node in nodes) {
            node.dropReferences { it.kind == RefKind.Owner }
            groupNode.append(node, RefKind.Origin)
            node.append(groupNode, RefKind.TopLevelPage)

            oldToNewNodeMap[node] = groupNode
        }

        // if nodes are classes, nested members should be also merged and
        // inserted at the same level with class
        if (nodes.all { it.kind == NodeKind.Class }) {
            val members = nodes.flatMap { it.allReferences() }.filter { it.kind == RefKind.Member }
            val mergedMembers = mergeMemberReferences(groupNode, members)

            for (ref in mergedMembers) {
                if (ref.kind == RefKind.Owner) {
                    continue
                }

                groupNode.append(ref.to, RefKind.Member)
            }
        }

        return groupNode
    }


    private fun mergeReferences(
        from: DocumentationNode,
        refs: List<DocumentationReference>
    ): List<DocumentationReference> {
        val (refsToPackages, otherRefs) = refs.partition { it.to.kind == NodeKind.Package }
        val mergedPackages = mergePackageReferences(from, refsToPackages)

        val (refsToMembers, refsNotToMembers) = otherRefs.partition { it.kind == RefKind.Member }
        val mergedMembers = mergeMemberReferences(from, refsToMembers)

        return mergedPackages + mergedMembers + refsNotToMembers
    }

    fun merge(): DocumentationModule {
        val mergedDocumentationModule = DocumentationModule(
            name = documentationModules.first().name,
            nodeRefGraph = producedNodeRefGraph
        )

        val refs = documentationModules.flatMap {
            it.allReferences()
        }
        mergeReferences(mergedDocumentationModule, refs)

        return mergedDocumentationModule
    }

    private fun updatePendingReferences() {
        for (ref in producedNodeRefGraph.references) {
            ref.lazyNodeFrom.update()
            ref.lazyNodeTo.update()
        }
    }

    private fun NodeResolver.update() {
        if (this is NodeResolver.Exact && exactNode in oldToNewNodeMap) {
            exactNode = oldToNewNodeMap[exactNode]!!
        }
    }
}