package org.jetbrains.dokka.javadoc.pages

import org.jetbrains.dokka.model.*

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

    // Void bound is currently used for return type only,
    // which is not used in the anchor generation, but
    // the handling for it is added regardless, just in case.
    // Note: if you accept `Void` as a param, it'll be a TypeConstructor
    Void -> "void"

    // Javadoc format currently does not support multiplatform projects,
    // so in an ideal world we should not see Dynamic here, but someone
    // might disable the checker or the support for it might be added
    // by Dokka or another plugin, so the handling is added just in case.
    Dynamic -> "dynamic"
}
