package org.jetbrains.dokka.utilities

fun <T : Any, S : Any> Pair<T?, S?>.pullOutNull(): Pair<T, S>? = first?.let { f -> second?.let { s -> f to s } }