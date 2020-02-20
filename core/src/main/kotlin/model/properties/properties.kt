package org.jetbrains.dokka.model.properties

interface Property<in C : Any> {
    interface Key<in C: Any, T: Any>
    val key: Key<C, *>
}
