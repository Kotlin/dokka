/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.impl.moduledocs


internal data class ModuleAndPackageDocumentationFragment(
    val name: String,
    val classifier: ModuleAndPackageDocumentation.Classifier,
    val documentation: String,
    val source: ModuleAndPackageDocumentationSource
)
