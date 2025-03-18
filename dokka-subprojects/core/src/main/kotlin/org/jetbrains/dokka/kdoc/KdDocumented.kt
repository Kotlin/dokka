/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.kdoc

public interface KdDocumented {
    public val description: KdDescription
}

public data class KdDescription(
    public val summary: KdDocumentationElement,
    public val detailed: KdDocumentationElement
)

// parsed markdown representation: text, links, etc
public sealed class KdDocumentationElement {
    public data class Text(public val string: String) : KdDocumentationElement()
}
