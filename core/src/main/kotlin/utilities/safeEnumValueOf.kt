package org.jetbrains.dokka.utilities

inline fun <reified T : Enum<*>> enumValueOrNull(name: String): T? =
    T::class.java.enumConstants.firstOrNull { it.name.toUpperCase() == name.toUpperCase() }