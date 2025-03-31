/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.kdoc

// represents a display name
public interface KdNamed {
    public val name: String
}

public interface KdDocumented {
    public val documentation: KdDocumentation
}

public data class KdDescription(
    public val summary: KdDocumentationElement,
    public val detailed: KdDocumentationElement
)

// parsed markdown representation: text, links, etc
public sealed class KdDocumentationElement {
    public data class Text(public val string: String) : KdDocumentationElement()
    public data class Paragraph(
        public val elements: KdDocumentationElement
    )
}

public interface KdDocumentation
//    (
//    public val content: List<KdDocumentationElement>,
//    public val tags: List<KdTag>
//)

public data class KdDocumentationParagraph(
    public val elements: KdDocumentationElement
)
