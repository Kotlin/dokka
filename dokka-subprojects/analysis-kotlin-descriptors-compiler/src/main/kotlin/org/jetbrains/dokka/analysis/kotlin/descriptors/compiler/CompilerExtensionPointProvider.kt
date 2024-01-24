/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.descriptors.compiler

import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.kotlin.extensions.ApplicationExtensionDescriptor

@InternalDokkaApi
public interface CompilerExtensionPointProvider {
    public fun get(): List<CompilerExtensionPoint>

    public class CompilerExtensionPoint(
        public val extensionDescriptor: ApplicationExtensionDescriptor<Any>,
        public val extensions: List<Any>
    )
}
