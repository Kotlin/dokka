/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.kdoc

// TODO: somehow reference modules (klibs.io/aggregated HTML)
public interface KdProject : KdDocumented {
    public val name: String
    public val topics: List<KdTopic>
}

public interface KdModule : KdDocumented {
    public val name: String
    public val fragments: List<KdFragment> // may be included into uklib/umanifest?
    public val symbols: List<KdSymbol>

    public val topics: List<KdTopic>
    public val samples: List<KdSample> // TODO?
}

public interface KdFragment {
    public val name: String // common, jvm, native, etc
    public val platforms: Set<String> // [jvm, iosX64, macosArm64, wasmJs, wasmWasi, ...]
    public val dependsOn: Set<String> // [ios, macos]
}

// TODO: later
public interface KdTopic {
    public val name: String
    public val content: KdDocumentationElement
}
