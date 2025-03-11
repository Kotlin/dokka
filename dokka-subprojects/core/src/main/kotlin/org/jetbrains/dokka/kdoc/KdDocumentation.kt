/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.kdoc

// parsed markdown representation: text, links, etc
public interface KdDocumentation {
    public val root: KdRenderNode
    // or
    public val nodes: List<KdRenderNode>
}

public interface KdRenderNode

public interface KdDocumented {
    public val description: KdDocumentation
}
