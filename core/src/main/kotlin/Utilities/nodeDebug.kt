package org.jetbrains.dokka.Utilities

import org.jetbrains.dokka.Model.DocumentationNode
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor

const val DOWN = '\u2503'
const val BRANCH = '\u2523'
const val LAST = '\u2517'

fun <T : DeclarationDescriptor> DocumentationNode<T>.pretty(prefix: String = "", isLast: Boolean = true): String {
    val nextPrefix = prefix + (if (isLast) ' ' else DOWN) + ' '

    return prefix + (if (isLast) LAST else BRANCH) + this.toString() +
            children.dropLast(1)
                .map { it.pretty(nextPrefix, false) }
                .plus(children.lastOrNull()?.pretty(nextPrefix))
                .filterNotNull()
                .takeIf { it.isNotEmpty() }
                ?.joinToString(prefix = "\n", separator = "")
                .orEmpty() + if (children.isEmpty()) "\n" else ""
}