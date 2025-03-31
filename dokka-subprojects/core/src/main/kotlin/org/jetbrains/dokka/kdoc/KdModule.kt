/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.kdoc

// TODO: somehow reference modules (klibs.io/aggregated HTML)
public interface KdProject : KdNamed, KdDocumented {
    public val topics: List<KdTopic>
    public val modules: List<KdModule>
}

public interface KdModule : KdNamed, KdDocumented {
    public val packages: List<KdPackage>
    public val fragments: List<KdFragment>
    public val samples: List<KdSample>
    public val topics: List<KdTopic>
}

public interface KdPackage : KdNamed, KdDocumented

public interface KdFragment : KdNamed {
    public val declarations: List<KdDeclaration>
}

// additional docs
public interface KdTopic : KdNamed, KdDocumented {
    public val path: String // or id
}
