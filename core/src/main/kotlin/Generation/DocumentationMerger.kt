package org.jetbrains.dokka.Generation

import org.jetbrains.dokka.*

class DocumentationMerger(
    private val documentationModules: List<DocumentationModule>
) {
    private val producedNodeRefGraph: NodeReferenceGraph
    private val signatureMap: Map<DocumentationNode, String>
    private val oldToNewNodeMap: MutableMap<DocumentationNode, DocumentationNode> = mutableMapOf()

    init {
        if (documentationModules.groupBy { it.name }.size > 1) {
            throw IllegalArgumentException("Modules should have similar names")
        }

        signatureMap = documentationModules
            .flatMap { it.nodeRefGraph.nodeMapView.entries }
            .associate { (k, v) -> v to k }


        producedNodeRefGraph = NodeReferenceGraph()
        documentationModules.map { it.nodeRefGraph }
            .flatMap { it.references }
            .distinct()
            .forEach { producedNodeRefGraph.addReference(it) }
    }

    private fun mergePackageReferences(
        from: DocumentationNode,
        packages: List<DocumentationReference>
    ): List<DocumentationReference> {

        val packagesByName = packages
            .map { it.to }
            .groupBy { it.name }

        val mutableList = mutableListOf<DocumentationReference>()
        for ((name, listOfPackages) in packagesByName) {
            val producedPackage = mergePackagesWithEqualNames(name, from, listOfPackages)
            updatePendingReferences()

            mutableList.add(
                DocumentationReference(from, producedPackage, RefKind.Member)
            )
        }

        return mutableList
    }

    private fun mergePackagesWithEqualNames(
        name: String,
        from: DocumentationNode,
        packages: List<DocumentationNode>
    ): DocumentationNode {
        val mergedPackage = DocumentationNode(name, Content.Empty, NodeKind.Package)
        for (packageNode in packages) {
            // TODO: Discuss
            mergedPackage.updateContent {
                for (otherChild in packageNode.content.children) {
                    children.add(otherChild)
                }
            }
            oldToNewNodeMap[packageNode] = mergedPackage
        }
        mergedPackage.clear()

        val references = packages.flatMap { it.allReferences() }
        val mergedReferences = mergeReferences(mergedPackage, references)
        for (ref in mergedReferences.distinct()) {
            mergedPackage.addReference(ref)
        }

        from.append(mergedPackage, RefKind.Member)

        return mergedPackage
    }

    private fun DocumentationNode.clear() = dropReferences { true }

    private fun mergeMembers(
        from: DocumentationNode,
        refs: List<DocumentationReference>
    ): List<DocumentationReference> {
        val membersBySignature: Map<String, List<DocumentationNode>> = refs.map { it.to }
            .groupBy { signatureMap[it]!! }

        val mergedMembers: MutableList<DocumentationReference> = mutableListOf()
        for ((signature, members) in membersBySignature) {
            val newNode = mergeMembersWithEqualSignature(signature, from, members)

            producedNodeRefGraph.register(signature, newNode)
            updatePendingReferences()
            from.append(newNode, RefKind.Member)

            mergedMembers.add(DocumentationReference(from, newNode, RefKind.Member))
        }

        return mergedMembers
    }

    private fun mergeMembersWithEqualSignature(
        signature: String,
        from: DocumentationNode,
        refs: List<DocumentationNode>
    ): DocumentationNode {
        val singleNode = refs.singleOrNull()
        if (singleNode != null) {
            singleNode.owner?.let { owner ->
                singleNode.dropReferences { it.to == owner && it.kind == RefKind.Owner }
            }
            return singleNode
        }
        val groupNode = DocumentationNode(refs.first().name, Content.Empty, NodeKind.GroupNode)
        groupNode.appendTextNode(signature, NodeKind.Signature, RefKind.Detail)

        for (node in refs) {
            if (node != groupNode) {
                node.owner?.let { owner ->
                    node.dropReferences { it.to == owner && it.kind == RefKind.Owner }
                    from.dropReferences { it.to == node && it.kind == RefKind.Member }
                }
                groupNode.append(node, RefKind.Member)

                oldToNewNodeMap[node] = groupNode
            }
        }
        return groupNode
    }


    private fun mergeReferences(
        from: DocumentationNode,
        refs: List<DocumentationReference>
    ): List<DocumentationReference> {
        val (refsToPackages, usualRefs) = refs.partition { it.to.kind == NodeKind.Package }
        val mergedPackages = mergePackageReferences(from, refsToPackages)

        val (refsToMembers, refsNotToMembers) = usualRefs.partition { it.kind == RefKind.Member }
        val mergedMembers = mergeMembers(from, refsToMembers)

        // TODO: think about
        return mergedPackages + (mergedMembers + refsNotToMembers).distinctBy {
            it.to.kind to it.to.name
        }
    }


    private fun updatePendingReferences() {
        producedNodeRefGraph.references.forEach {
            it.lazyNodeFrom.update()
            it.lazyNodeTo.update()
        }
    }

    private fun NodeResolver.update() {
        if (this is NodeResolver.Exact) {
            if (exactNode != null && oldToNewNodeMap.containsKey(exactNode!!)) {
                exactNode = oldToNewNodeMap[exactNode!!]
            }
        }
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


}