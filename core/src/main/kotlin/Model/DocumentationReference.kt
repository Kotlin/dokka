package org.jetbrains.dokka

import com.google.inject.Singleton

enum class RefKind {
    Owner,
    Member,
    InheritedMember,
    InheritedCompanionObjectMember,
    Detail,
    Link,
    HiddenLink,
    Extension,
    Inheritor,
    Superclass,
    Override,
    Annotation,
    HiddenAnnotation,
    Deprecation,
    TopLevelPage,
    Platform,
    ExternalType,
    Origin,
    SinceKotlin
}

data class DocumentationReference(val from: DocumentationNode, val to: DocumentationNode, val kind: RefKind) {
}

sealed class NodeResolver {
    abstract fun resolve(nodeRephGraph: NodeReferenceGraph): DocumentationNode?
    class BySignature(var signature: String) : NodeResolver() {
        override fun resolve(nodeRephGraph: NodeReferenceGraph): DocumentationNode? {
            return nodeRephGraph.lookup(signature)
        }
    }

    class Exact(var exactNode: DocumentationNode) : NodeResolver() {
        override fun resolve(nodeRephGraph: NodeReferenceGraph): DocumentationNode? {
            return exactNode
        }
    }
}

class PendingDocumentationReference(val lazyNodeFrom: NodeResolver,
                                    val lazyNodeTo: NodeResolver,
                                    val kind: RefKind) {
    fun resolve(nodeRephGraph: NodeReferenceGraph) {
        val fromNode = lazyNodeFrom.resolve(nodeRephGraph)
        val toNode = lazyNodeTo.resolve(nodeRephGraph)
        if (fromNode != null && toNode != null) {
            fromNode.addReferenceTo(toNode, kind)
        }
    }
}

class NodeReferenceGraph {
    private val nodeMap = hashMapOf<String, DocumentationNode>()
    val nodeMapView: Map<String, DocumentationNode>
            get() = HashMap(nodeMap)

    val references = arrayListOf<PendingDocumentationReference>()

    fun register(signature: String, node: DocumentationNode) {
        nodeMap[signature] = node
    }

    fun link(fromNode: DocumentationNode, toSignature: String, kind: RefKind) {
        references.add(
            PendingDocumentationReference(
                NodeResolver.Exact(fromNode),
                NodeResolver.BySignature(toSignature),
                kind
            ))
    }

    fun link(fromSignature: String, toNode: DocumentationNode, kind: RefKind) {
        references.add(
            PendingDocumentationReference(
                NodeResolver.BySignature(fromSignature),
                NodeResolver.Exact(toNode),
                kind
            )
        )
    }

    fun link(fromSignature: String, toSignature: String, kind: RefKind) {
        references.add(
            PendingDocumentationReference(
                NodeResolver.BySignature(fromSignature),
                NodeResolver.BySignature(toSignature),
                kind
            )
        )
    }

    fun addReference(reference: PendingDocumentationReference) {
        references.add(reference)
    }

    fun lookup(signature: String) = nodeMap[signature]

    fun lookupOrWarn(signature: String, logger: DokkaLogger): DocumentationNode? {
        val result = nodeMap[signature]
        if (result == null) {
            logger.warn("Can't find node by signature `$signature`." +
                    "This is probably caused by invalid configuration of cross-module dependencies")
        }
        return result
    }

    fun resolveReferences() {
        references.forEach { it.resolve(this) }
    }
}

@Singleton
class PlatformNodeRegistry {
    private val platformNodes = hashMapOf<String, DocumentationNode>()

    operator fun get(platform: String): DocumentationNode {
        return platformNodes.getOrPut(platform) {
            DocumentationNode(platform, Content.Empty, NodeKind.Platform)
        }
    }
}
