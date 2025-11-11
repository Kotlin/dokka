/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

public sealed class KdDocumented {
    public abstract val documentation: KdDocumentation?
}

@Serializable
public data class KdDocumentation(
    public val elements: List<KdDocumentationElement>
)

// parsed markdown representation: text, links, etc
@Serializable
public sealed class KdDocumentationElement {
    @SerialName("text")
    @Serializable
    public data class Text(public val value: String) : KdDocumentationElement()

    @SerialName("code")
    @Serializable
    public data class Code(
        public val lines: List<String>,
        public val language: String? = null
    ) : KdDocumentationElement()

    @SerialName("parameter-link")
    @Serializable
    public data class ParameterLink(public val parameterName: String /* this -> receiver */) : KdDocumentationElement()

    @SerialName("type-parameter-link")
    @Serializable
    public data class TypeParameterLink(public val typeParameterName: String) : KdDocumentationElement()

    @SerialName("reference-link")
    @Serializable
    public data class ReferenceLink(public val elementId: KdElementId) : KdDocumentationElement()

    // TODO: multi-reference, when we have ambiguity
    @SerialName("ambiguity-reference-link")
    @Serializable
    public data class AmbiguityReferenceLink(public val elementIds: List<KdElementId>) : KdDocumentationElement()

    @SerialName("external-link")
    @Serializable
    public data class ExternalLink(public val url: String) : KdDocumentationElement()

    @SerialName("paragraph")
    @Serializable
    public data class Paragraph(public val elements: List<KdDocumentationElement>) : KdDocumentationElement()
}

// tags:
// - ref + optional docs (@param, @throws)
// - docs (@return, @since, @author)
// - @see - TBD - multiple syntaxes
// - ref (@sample)

// KDoc tags (add JavaDoc tags)
// AUTHOR - PLAIN TAG
// SINCE - PLAIN TAG
// SEE - REFERENCE TAG (TBD)

// those tags will not map to `KdTag` at all

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
public data class KdDocumentationTag(
    public val name: String,
    public val reference: KdElementId? = null, // TODO: what type should be here?
    override val documentation: KdDocumentation? = null,
    // public val isCustom: Boolean?
) : KdDocumented()
