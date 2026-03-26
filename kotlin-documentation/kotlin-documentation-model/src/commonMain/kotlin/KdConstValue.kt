/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.Serializable

// TODO: simplify for now
@Serializable
public class KdConstValue(
    public val value: String,
)

// used by annotations, const properties, default values
//@Serializable
//public sealed class KdConstValue {
//    @SerialName("null")
//    @Serializable
//    public data object Null : KdConstValue()
//
//    @SerialName("byte")
//    @Serializable
//    public data class Byte(public val value: kotlin.Byte) : KdConstValue()
//
//    @SerialName("ubyte")
//    @Serializable
//    public data class UByte(public val value: kotlin.UByte) : KdConstValue()
//    // Char, Int, Short, Long, Float, Double
//
//    @SerialName("string")
//    @Serializable
//    public data class String(public val value: kotlin.String) : KdConstValue()
//}
