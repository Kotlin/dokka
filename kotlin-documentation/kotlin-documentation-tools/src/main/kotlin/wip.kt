/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation.tools

import org.jetbrains.kotlin.documentation.KdFragment
import java.nio.file.Path

public class KdInputFragment(
    public val name: String,
    public val sources: List<Path>,
    // TODO: probably we should support both binary dependencies and machine readable format here
    //  most likely in reality both will be needed: binary dependencies for AA work and machine-readable-format for documentation and cross-links
    public val classpath: List<Path>,
    public val samples: List<Path>,
    public val dependsOn: List<String>
    // path to packages/docs a.k.a. includes

    // visibility filter
    //
)

// TODO: split api vs impl as in buildtools and abi generator???
public fun analyze(
    fragmentInfo: List<KdInputFragment>,
    warnOnUndocumented: Boolean = true, // better suppress system
    warningsAsErrors: Boolean = false,
    skipDeprecated: Boolean // TODO: remove it?
): List<KdFragment> {
    TODO()
}

// filters:
// - by visibility
// - by deprecation
// - by opt-in
// - by package