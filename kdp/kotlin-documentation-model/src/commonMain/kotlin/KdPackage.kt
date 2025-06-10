/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.Serializable

@Serializable
public data class KdPackageId(
    public val packageName: String
) : KdElementId()

// TODO: some other metadata could go here from YAML frontmatter ???
@Serializable
public data class KdPackage(
    override val id: KdPackageId,
    override val documentation: KdDocumentation
) : KdElement()
