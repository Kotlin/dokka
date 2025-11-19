/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

public sealed interface KdDocumented {
    public val documentation: KdDocumentation?
}

// TODO: we could just inline this into `KdDocumented`, having `documentationNodes`
@Serializable
public data class KdDocumentation(
    public val elements: List<KdDocumentationNode>
)

// parsed markdown representation: text, links, etc
@Serializable
public sealed class KdDocumentationNode {
    @SerialName("text")
    @Serializable
    public data class Text(public val value: String) : KdDocumentationNode()

    @SerialName("code")
    @Serializable
    public data class Code(
        public val lines: List<String>,
        public val language: String? = null
    ) : KdDocumentationNode()

    @Serializable
    public data class Link(
        val label: KdDocumentationNode,
        val reference: KdLinkReference,
    ) : KdDocumentationNode()

    @SerialName("external-link")
    @Serializable
    public data class ExternalLink(
        val label: KdDocumentationNode,
        val url: String
    ) : KdDocumentationNode()

    @SerialName("paragraph")
    @Serializable
    public data class Paragraph(public val children: List<KdDocumentationNode>) : KdDocumentationNode()

    // -------------------------

    // tags:
    // - ref + optional docs (@param, @throws)
    // - docs (@return, @since, @author)
    // - @see - TBD - multiple syntaxes
    // - ref (@sample)

    // KDoc tags (add JavaDoc tags)
    // AUTHOR - PLAIN TAG
    // SINCE - PLAIN TAG
    // SEE - REFERENCE TAG (TBD)

    // those tags will not map to `Tag` at all

    // RETURN - PLAIN TAG -> transforms into docs for return value
    // THROWS/EXCEPTION - REFERENCE TAG -> transforms into docs for throw
    // RECEIVER - PLAIN TAG -> transforms into docs for receiver
    // PARAM - REFERENCE TAG -> transforms into docs for parameter
    // CONSTRUCTOR - CONTEXT TAG -> transforms into docs for constructor
    // PROPERTY - CONTEXT TAG -> transforms into docs for property
    // SAMPLE - SPECIAL TAG -> TODO
    // SUPPRESS - SPECIAL TAG -> skips declaration

    // TODO: may be reference should also sometimes allow external links
    // TODO: may be another name
    @Serializable
    public data class Tag(
        val name: String,
        val reference: KdLinkReference? = null, // TODO: what type should be here?
        val children: List<KdDocumentationNode> = emptyList(),
    ) : KdDocumentationNode()

}

// TODO: naming, values
@Serializable
public sealed class KdLinkReference {
    @Serializable
    public data object This : KdLinkReference()

    @Serializable
    public data class Parameter(val name: String) : KdLinkReference()

    @Serializable
    public data class TypeParameter(val name: String) : KdLinkReference()

    @Serializable
    public data class Package(val packageName: String) : KdLinkReference()

    @Serializable
    public data class Callable(val callableId: KdCallableId) : KdLinkReference()

    @Serializable
    public data class Classifier(val classifierId: KdClassifierId) : KdLinkReference()
}
