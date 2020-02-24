package org.jetbrains.dokka.model.properties

interface Property<in C : Any> {
    interface Key<in C: Any, T: Any>
    val key: Key<C, *>
}

interface CalculatedProperty<in C: Any, T: Any>: Property.Key<C, T> {
    fun calculate(subject: C): T
}