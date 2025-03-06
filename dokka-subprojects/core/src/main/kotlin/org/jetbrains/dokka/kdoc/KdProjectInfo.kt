/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.kdoc

// question to uklibs
// module = org.jetbrains.kotlinx:kotlinx-coroutines-core
// version = 1.0

// THERE IS NO MODULE DEFINITION NOW or project definition now in Kotlin
// probably module should be package manager specific

public interface KdProjectInfo : KdDocumented {
    public val name: String
    public val modules: List<KdModuleInfo>
}

public interface KdModuleInfo : KdDocumented {
    public val name: String // displayName
    public val packages: List<KdPackageInfo>
    public val fragments: List<KdFragmentInfo>
    // public val files: List<KdFileInfo>
}

public interface KdPackageInfo : KdDocumented {
    public val name: String
}

public interface KdFileInfo {
    // TODO?
}

public interface KdFragmentInfo {
    public val name: String
    public val dependsOn: Set<String> // fragment names
    public val platforms: Set<String>
}


// other things, later

public interface KdArticle : KdDocumented {
    public val name: String
}

public interface KdSample {
    public val name: String
}
