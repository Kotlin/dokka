/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.kdoc

// TODO: may be it's not really needed :)
public interface KdProject : KdDocumented {
    public val name: String
    public val modules: List<KdModule>
}

// question to uklibs
// module = org.jetbrains.kotlinx:kotlinx-coroutines-core
// version = 1.0

// THERE IS NO MODULE DEFINITION NOW or project definition now in Kotlin
// probably module should be package manager specific

public interface KdModule : KdDocumented {
//    public val name: String
//    public val version: String

    public val packages: Map<KdSymbolId, KdPackage>
    public val fragments: List<KdFragment>
    public val topics: List<KdTopic>
}

public interface KdFragment {
    public val name: String // common, jvm, native, etc
//    public val platforms: Set<String>
//    public val dependsOn: Set<String>

    public val declarations: Map<KdSymbolId, KdDeclaration>
    // public val samples: List<KdSample>
}

public interface KdPackage : KdDocumented {
    public val name: String
}

public interface KdTopic : KdDocumented {
    public val name: String
}

public interface KdSample : KdDocumented {
    public val name: String
}
