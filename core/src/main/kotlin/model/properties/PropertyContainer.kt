package org.jetbrains.dokka.model.properties

class PropertyContainer<C : Any> private constructor(
    @PublishedApi internal val map: Map<Property.Key<C, *>, Property<C>>
) {
    operator fun <D : C> plus(prop: Property<D>): PropertyContainer<D> =
        PropertyContainer(map + (prop.key to prop))

    // TODO: Add logic for caching calculated properties
    inline operator fun <reified T: Any> get(key: Property.Key<C, T>): T? = when (val prop = map[key]) {
        is T? -> prop
        else -> throw ClassCastException("Property for $key stored under not matching key type.")
    }

    companion object {
        val empty: PropertyContainer<Any> = PropertyContainer(emptyMap())
    }
}