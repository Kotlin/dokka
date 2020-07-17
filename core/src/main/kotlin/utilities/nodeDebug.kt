package org.jetbrains.dokka.utilities

import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.pages.*

const val DOWN = '\u2503'
const val BRANCH = '\u2523'
const val LAST = '\u2517'

fun Documentable.pretty(prefix: String = "", isLast: Boolean = true): String {
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

//fun Any.genericPretty(prefix: String = "", isLast: Boolean = true): String {
//    val nextPrefix = prefix + (if (isLast) ' ' else DOWN) + ' '
//
//    return prefix + (if (isLast) LAST else BRANCH) + this.stringify() +
//            allChildren().dropLast(1)
//                .map { it.genericPretty(nextPrefix, false) }
//                .plus(allChildren().lastOrNull()?.genericPretty(nextPrefix))
//                .filterNotNull()
//                .takeIf { it.isNotEmpty() }
//                ?.joinToString(prefix = "\n", separator = "")
//                .orEmpty() + if (allChildren().isEmpty()) "\n" else ""
//}
private fun Any.stringify() = when(this) {
    is ContentNode -> toString() + this.dci
    is ContentPage -> this.name + this::class.simpleName
    else -> toString()
}
//private fun Any.allChildren() = when(this){
//    is PageNode -> children + content
//    is ContentBlock -> this.children
//    is ContentHeader -> this.items
//    is ContentStyle -> this.items
//    is ContentSymbol -> this.parts
//    is ContentComment -> this.parts
//    is ContentGroup -> this.children
//    is ContentList -> this.items
//    else -> emptyList()
//}
