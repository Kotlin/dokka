/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SerialName("module")
@Serializable
public data class KdModule(
    override val id: KdModuleId,
    override val name: String,
    val packages: List<KdPackageId> = emptyList(),
    override val documentation: List<KdDocumentationNode> = emptyList(),
) : KdElement()
