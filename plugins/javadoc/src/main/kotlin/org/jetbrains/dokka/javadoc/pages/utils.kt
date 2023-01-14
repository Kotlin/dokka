package org.jetbrains.dokka.javadoc.pages

import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.utilities.htmlEscape

internal fun JavadocFunctionNode.getAnchor(): String {
    val parameters = parameters.joinToString(",") { it.typeBound.asString() }
    return "$name($parameters)".htmlEscape()
}

private fun Bound.asString(): String = when (this) {
    is Nullable -> this.inner.asString()
    is DefinitelyNonNullable -> this.inner.asString()
    is TypeConstructor -> listOf(this.dri.packageName, this.dri.classNames).joinToString(".")
    is TypeParameter -> this.name
    is PrimitiveJavaType -> this.name
    is UnresolvedBound -> this.name
    is TypeAliased -> this.typeAlias.asString()
    is JavaObject -> "Object"
    Dynamic -> "dynamic"
    Void -> "void"
}