/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.generation.kdp

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.DRIExtraContainer
import org.jetbrains.dokka.links.EnumEntryDRIExtra
import org.jetbrains.kotlin.documentation.KdCallableId
import org.jetbrains.kotlin.documentation.KdClassifierId

internal fun DRI.toKdClassifierId(): KdClassifierId = KdClassifierId(
    packageName = requireNotNull(packageName) { "packageName is null for $this" },
    classNames = requireNotNull(classNames) { "classNames is null for $this" },
)

internal fun DRI.toKdCallableId(): KdCallableId {
    val packageName = requireNotNull(packageName) { "packageName is null for $this" }

    // enum entry
    return if (extra != null && DRIExtraContainer(extra)[EnumEntryDRIExtra] != null) {
        val pseudoClassNames = requireNotNull(classNames) { "classNames is null for $this" }
        val classNames = pseudoClassNames.substringBeforeLast('.', "")
        val callableName = pseudoClassNames.substringAfterLast('.', "")
        require(classNames.isNotBlank()) { "classNames is blank for $this" }
        require(callableName.isNotBlank()) { "callableName is blank for $this" }

        KdCallableId(
            packageName = packageName,
            classNames = classNames,
            callableName = callableName
        )
    } else {
        val callable = requireNotNull(callable) { "callable is null for $this" }
        KdCallableId(
            packageName = packageName,
            classNames = classNames,
            callableName = callable.name
        )
    }
}
