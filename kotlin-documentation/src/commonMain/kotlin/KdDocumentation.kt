/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

public sealed class KdDocumented {
    public abstract val documentation: KdDocumentation?
}

public interface KdDocumentation

// parsed markdown representation: text, links, etc
public sealed class KdDocumentationElement {
    public data class Text(public val value: String) : KdDocumentationElement()
    public data class Link(public val elementId: KdElementId) : KdDocumentationElement()
    public data class Paragraph(public val elements: List<KdDocumentationElement>) : KdDocumentationElement()
}
