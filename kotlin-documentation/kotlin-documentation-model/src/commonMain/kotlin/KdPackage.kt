/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.Serializable

// TODO: some other metadata could go here from YAML frontmatter ???
@Serializable
public data class KdPackage(
    override val id: KdPackageId,
    override val documentation: KdDocumentation? = null,
    public val classifiers: List<KdClassifier> = emptyList(),
    public val callables: List<KdCallable> = emptyList(),
) : KdElement()
