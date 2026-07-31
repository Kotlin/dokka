/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// in java -> from package-info.java
// in kotlin -> TBD
@SerialName("package")
@Serializable
public data class KdPackage(
    override val id: KdPackageId,
    override val name: String,
    val classlikes: List<KdClassLikeId> = emptyList(),
    val callables: List<KdCallableId> = emptyList(),
    override val documentation: List<KdDocumentationNode> = emptyList(),
) : KdElement()
