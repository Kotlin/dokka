package org.jetbrains.dokka.gradle

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.property
import org.jetbrains.dokka.utilities.cast
import kotlin.reflect.typeOf

internal inline fun <reified T : Any> ObjectFactory.safeProperty() = property<T?>()

internal inline fun <reified T : Any> Property<T?>.safeConvention(value: T): Property<T> {
    @Suppress("UnstableApiUsage")
    return this.convention(value).cast()
}

@OptIn(ExperimentalStdlibApi::class)
internal inline fun <reified T> Provider<T>.getSafe(): T =
    if (typeOf<T>().isMarkedNullable) orNull as T
    else get()

