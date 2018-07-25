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
    ExternalType
}

data class DocumentationReference(val from: DocumentationNode, val to: DocumentationNode, val kind: RefKind) {
}

sealed class NodeResolver {
    abstract fun resolve(): DocumentationNode?
    class BySignature(var signature: String, var nodeMap: Map<String, DocumentationNode>) : NodeResolver() {
        override fun resolve(): DocumentationNode? {
            return nodeMap[signature]
        }
    }

    class Exact(var exactNode: DocumentationNode?) : NodeResolver() {
        override fun resolve(): DocumentationNode? {
            return exactNode
        }
    }
}

class PendingDocumentationReference(val lazyNodeFrom: NodeResolver,
                                    val lazyNodeTo: NodeResolver,
                                    val kind: RefKind) {
    fun resolve() {
        val fromNode = lazyNodeFrom.resolve()
        val toNode = lazyNodeTo.resolve()
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
                NodeResolver.BySignature(toSignature, nodeMap),
                kind
            ))
    }

    fun link(fromSignature: String, toNode: DocumentationNode, kind: RefKind) {
        references.add(
            PendingDocumentationReference(
                NodeResolver.BySignature(fromSignature, nodeMap),
                NodeResolver.Exact(toNode),
                kind
            )
        )
    }

    fun link(fromSignature: String, toSignature: String, kind: RefKind) {
        references.add(
            PendingDocumentationReference(
                NodeResolver.BySignature(fromSignature, nodeMap),
                NodeResolver.BySignature(toSignature, nodeMap),
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
            logger.warn("Can't find node by signature `$signature`")
        }
        return result
    }

    fun resolveReferences() {
        references.forEach { it.resolve() }
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
