/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

// TODO: Kotlin doesn't have yet some moduleName data at all, but could have soon!
//@Serializable
//public data class KdModuleId(
//    public val moduleName: String
//) : KdElementId()
//
//// (package on klibs.io), (artifact in maven central), (gradle project)
//// or just library, which the user adds to the project
//@Serializable
//public data class KdModule(
//    override val id: KdModuleId,
//    override val documentation: KdDocumentation? = null,
//    public val fragments: List<KdFragment> = emptyList(),
//) : KdElement()
//
//// TODO: What to do here?
//// TODO: aggregation - for now skip
//// when publishing it could be attached to bom or version-catalog or docs artifact???
//@Serializable
//public data class KdProject(
////    public val modules: List<KdModule>,
//    public val topics: List<KdTopic>
//)
