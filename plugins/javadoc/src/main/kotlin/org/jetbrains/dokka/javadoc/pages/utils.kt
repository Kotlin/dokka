package org.jetbrains.dokka.javadoc.pages

import org.jetbrains.dokka.model.*

internal fun JavadocFunctionNode.getAnchor(): String =
    "$name(${parameters.joinToString(",") {
        when (val bound = if (it.typeBound is Nullable) it.typeBound.inner else it.typeBound) {
            is TypeConstructor -> listOf(bound.dri.packageName, bound.dri.classNames).joinToString(".")
            is TypeParameter -> bound.name
            is PrimitiveJavaType -> bound.name
            is UnresolvedBound -> bound.name
            is JavaObject -> "Object"
            else -> bound.toString()
        }
    }})"