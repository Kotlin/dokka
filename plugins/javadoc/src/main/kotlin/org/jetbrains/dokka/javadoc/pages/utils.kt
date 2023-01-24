package org.jetbrains.dokka.javadoc.pages

import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.utilities.htmlEscape

/**
 * Returns an unencoded, unescaped function anchor.
 *
 * Should be URL encoded / HTML escaped at call site,
 * depending on usage.
 */
// see the discussion in #2813 related to encoding/escaping this value for ids/hrefs
internal fun JavadocFunctionNode.getAnchor(): String {
    val parameters = parameters.joinToString(",") { it.typeBound.asString() }
    return "$name($parameters)"
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