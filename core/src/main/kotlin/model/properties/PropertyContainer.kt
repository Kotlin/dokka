package org.jetbrains.dokka.model.properties

import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

class PropertyContainer<C : Any> internal constructor(
    @PublishedApi internal val map: Map<ExtraProperty.Key<C, *>, ExtraProperty<C>>
) {
    operator fun <D : C> plus(prop: ExtraProperty<D>): PropertyContainer<D> =
        PropertyContainer(map + (prop.key to prop))

    // TODO: Add logic for caching calculated properties
    inline operator fun <reified T : Any> get(key: ExtraProperty.Key<C, T>): T? = when (val prop = map[key]) {
        is T? -> prop
        else -> throw ClassCastException("Property for $key stored under not matching key type.")
    }

    companion object {
        fun <T: Any> empty(): PropertyContainer<T> = PropertyContainer(emptyMap())
    }
}

interface WithExtraProperties<C : Any> {
    val extra: PropertyContainer<C>

    fun withNewExtras(newExtras: PropertyContainer<C>): C
}

fun <C> C.mergeExtras(left: C, right: C): C where C : Any, C : WithExtraProperties<C> {
    val aggregatedExtras: List<List<ExtraProperty<C>>> =
        (left.extra.map.values + right.extra.map.values)
            .groupBy { it.key }
            .values
            .map { it.distinct() }

    val (unambiguous, toMerge) = aggregatedExtras.partition { it.size == 1 }

    @Suppress("UNCHECKED_CAST")
    val strategies: List<MergeStrategy<C>> = toMerge.map { (l, r) ->
        (l.key as ExtraProperty.Key<C, ExtraProperty<C>>).mergeStrategyFor(l, r)
    }

    strategies.firstIsInstanceOrNull<MergeStrategy.Fail>()?.error?.invoke()

    val replaces: List<ExtraProperty<C>> = strategies.filterIsInstance<MergeStrategy.Replace<C>>().map { it.newProperty }

    val needingFullMerge: List<(preMerged: C, left: C, right: C) -> C> =
        strategies.filterIsInstance<MergeStrategy.Full<C>>().map { it.merger }

    val newExtras = PropertyContainer((unambiguous.flatten() + replaces).associateBy { it.key })

    return needingFullMerge.fold(withNewExtras(newExtras)) { acc, merger -> merger(acc, left, right) }
}