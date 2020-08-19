package org.jetbrains.dokka.gradle

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.provider.HasMultipleValues
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.util.Path
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget

internal infix fun <T> Property<T>.by(value: T?) {
    this.set(value)
}

internal infix fun <T> Property<T>.by(value: Provider<T>) {
    this.set(value)
}

internal infix fun <T> HasMultipleValues<in T>.by(values: Iterable<T>) {
    this.set(values)
}

internal infix fun <T> HasMultipleValues<in T>.by(values: Provider<out Iterable<T>>) {
    this.set(values)
}

internal fun parsePath(path: String): Path = Path.path(path)

internal val Project.kotlinOrNull: KotlinProjectExtension?
    get() = try {
        project.extensions.findByType(KotlinProjectExtension::class.java)
    } catch (e: NoClassDefFoundError) {
        null
    }

internal val Project.kotlin: KotlinProjectExtension
    get() = project.extensions.getByType(KotlinProjectExtension::class.java)

internal fun Project.isAndroidProject() = try {
    project.extensions.getByName("android")
    true
} catch (e: UnknownDomainObjectException) {
    false
} catch (e: ClassNotFoundException) {
    false
}

internal fun KotlinTarget.isAndroidTarget() = this.platformType == KotlinPlatformType.androidJvm

internal fun <T : Any> NamedDomainObjectContainer<T>.maybeCreate(name: String, configuration: T.() -> Unit): T {
    return findByName(name) ?: create(name, configuration)
}
