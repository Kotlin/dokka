/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.Serializable

@Serializable
public sealed class KdClassLike : KdDeclaration() {
    abstract override val id: KdClassLikeId
}

// TODO: enum is one more case? or we could represent entries as `static` variables? valueOf is `static` function, entries is `static` variable

// class can have `declarations` inside, typealias - just typealias
// all class kinds can have declarations inside
// class, interface, object
// enum class
// annotation class
// data class, data object
// value class, value object,
// companion object
// record (java)


