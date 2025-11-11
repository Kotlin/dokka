/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.Serializable

// pkg:org.example
// cls:org.example/ClassA
// call:org.example/ClassA/prop
// call:org.example/ClassA/fun/123 - 123 is a hash
// call:org.example//prop - top-level
// call:org.example//fun/123

@Serializable
public sealed class KdElementId

@Serializable
public data class KdPackageId(
    public val packageName: String
) : KdElementId()

@Serializable
public sealed class KdDeclarationId : KdElementId() {
    public abstract val packageName: String

    public val packageId: KdPackageId get() = KdPackageId(packageName)
}

// represents: constructor (no name), function, property, enum_entry
@Serializable
public data class KdCallableId(
    override val packageName: String,
    public val classNames: String?, // if null -> top-level
    public val callableName: String?, // if null -> constructor
    public val parametersHash: String? // if null -> no parameters? -> variable
) : KdDeclarationId() {
    public val classifierId: KdClassifierId? get() = classNames?.let { KdClassifierId(packageName, it) }
}

@Serializable
public data class KdClassifierId(
    override val packageName: String,
    public val classNames: String, // it could be A.B.C for nested class
) : KdDeclarationId()

@Serializable
public sealed class KdElement : KdDocumented() {
    public abstract val id: KdElementId
}
