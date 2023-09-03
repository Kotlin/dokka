/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.impl

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.PointingToDeclaration
import org.jetbrains.dokka.analysis.kotlin.internal.KotlinToJavaService
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

internal class DescriptorKotlinToJavaMapper : KotlinToJavaService {

    override fun findAsJava(kotlinDri: DRI): DRI? {
        return kotlinDri.partialFqName().mapToJava()?.toDRI(kotlinDri)
    }

    private fun DRI.partialFqName() = packageName?.let { "$it." } + classNames

    private fun String.mapToJava(): ClassId? =
        JavaToKotlinClassMap.mapKotlinToJava(FqName(this).toUnsafe())

    private fun ClassId.toDRI(dri: DRI?): DRI = DRI(
        packageName = packageFqName.asString(),
        classNames = classNames(),
        callable = dri?.callable,//?.asJava(), TODO: check this
        extra = null,
        target = PointingToDeclaration
    )

    private fun ClassId.classNames(): String =
        shortClassName.identifier + (outerClassId?.classNames()?.let { ".$it" } ?: "")
}
