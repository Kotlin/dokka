package org.jetbrains.dokka.base.renderers

import org.jetbrains.dokka.pages.ContentKind
import org.jetbrains.dokka.pages.ContentNode
import org.jetbrains.dokka.pages.Kind
import org.jetbrains.dokka.utilities.DokkaLogger

class DefaultTabSortingStrategy : TabSortingStrategy {
    override fun <T: ContentNode> sort(tabs: Collection<T>): List<T> {
        val tabMap: Map<Kind, MutableList<T>> = mapOf(
            ContentKind.Classlikes to mutableListOf(),
            ContentKind.Constructors to mutableListOf(),
            ContentKind.Functions to mutableListOf(),
            ContentKind.Properties to mutableListOf(),
            ContentKind.Extensions to mutableListOf(),
            ContentKind.Parameters to mutableListOf(),
            ContentKind.Inheritors to mutableListOf(),
            ContentKind.Source to mutableListOf(),
            ContentKind.Sample to mutableListOf(),
            ContentKind.Comment to mutableListOf()
        )
        val unrecognized: MutableList<T> = mutableListOf()
        tabs.forEach {
            tabMap[it.dci.kind]?.add(it) ?: unrecognized.add(it)
        }
        return tabMap.values.flatten() + unrecognized
    }

}