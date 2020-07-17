package org.jetbrains.dokka.base.renderers

import org.jetbrains.dokka.pages.ContentKind
import org.jetbrains.dokka.pages.ContentNode
import org.jetbrains.dokka.pages.Kind
import org.jetbrains.dokka.utilities.DokkaLogger

private val kindsOrder = listOf(
    ContentKind.Classlikes,
    ContentKind.Constructors,
    ContentKind.Functions,
    ContentKind.Properties,
    ContentKind.Extensions,
    ContentKind.Parameters,
    ContentKind.Inheritors,
    ContentKind.Source,
    ContentKind.Sample,
    ContentKind.Comment
)

class DefaultTabSortingStrategy : TabSortingStrategy {
    override fun <T: ContentNode> sort(tabs: Collection<T>): List<T> {
        val tabMap: Map<Kind, MutableList<T>> = kindsOrder.asSequence().map { it to mutableListOf<T>() }.toMap()
        val unrecognized: MutableList<T> = mutableListOf()
        tabs.forEach {
            tabMap[it.dci.kind]?.add(it) ?: unrecognized.add(it)
        }
        return tabMap.values.flatten() + unrecognized
    }

}
