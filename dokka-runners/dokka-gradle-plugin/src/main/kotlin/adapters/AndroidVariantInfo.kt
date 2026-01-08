/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.adapters

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.Variant
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.jetbrains.dokka.gradle.internal.findExtensionLenient
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation

/**
 * Store details about a [Variant].
 *
 * @param[name] [Variant.name].
 * @param[isPublishable] `true` if any component of the variant is 'published',
 * i.e. it is an instance of [Variant].
 * @param[compileClasspath] The value of [Variant.compileClasspath].
 * @param[kotlinSources] Kotlin source directories [com.android.build.api.variant.Sources.kotlin].
 * @param[javaSources] Java source directories [com.android.build.api.variant.Sources.java].
 */
internal data class AndroidVariantInfo(
    val name: String,
    val isPublishable: Boolean,
    val compileClasspath: FileCollection,
    val kotlinSources: Provider<out Collection<Directory>>?,
    val javaSources: Provider<out Collection<Directory>>?,
)


/** Try and get [AndroidComponentsExtension], or `null` if it's not present. */
internal fun Project.findAndroidComponentsExtension(): AndroidComponentsExtension<*, *, *>? =
    findExtensionLenient<AndroidComponentsExtension<*, *, *>>("androidComponents")


/**
 * Collect [AndroidVariantInfo]s of the Android [Variant]s in this Android project.
 *
 * We store the collected data in a custom class to aid with Configuration Cache compatibility.
 *
 * This function must only be called when AGP is applied
 * (otherwise [findAndroidComponentsExtension] will return `null`),
 * i.e. inside a `withPlugin(...) {}` block.
 *
 * ### tl;dr:
 *
 * 1. Fetch all [Variant]s from [AndroidComponentsExtension].
 * 2. Check if a `Variant` is publishable:
 *   are any of its [com.android.build.api.variant.Component]s publishable?
 *   A `Component` is publishable if it is an instance of `Variant` *and* [Variant.buildType] is `release`.
 * 3. Each [KotlinCompilation] is associated with a single `Variant`.
 * 4. Each [org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet]
 *   has an associated list of `KotlinCompilation`s.
 *
 * Therefore, Dokka must simply check if any of the
 * `Component`s associated with a `KotlinSourceSet` are publishable.
 *
 * ### How to determine publishability of AGP Variants
 *
 * There are several Android Gradle plugins.
 * Each AGP has a specific associated [Variant]:
 * - `com.android.application` - [com.android.build.api.variant.ApplicationVariant]
 * - `com.android.library` - [com.android.build.api.variant.DynamicFeatureVariant]
 * - `com.android.test` - [com.android.build.api.variant.LibraryVariant]
 * - `com.android.dynamic-feature` - [com.android.build.api.variant.TestVariant]
 *
 * (`com.android.base` is the shared base of these plugins.)
 *
 * Each [Variant] has associated [com.android.build.api.variant.Component]s.
 * Each `Component` is associated with a single [KotlinCompilation].
 *
 * So, we can determine if a [KotlinCompilation] is 'publishable' by checking the associated
 * `Component` is 'publishable'.
 *
 * We consider a `Component` as publishable if:
 *
 * 1. It is an instance of [Variant] (e.g. it is a [com.android.build.api.variant.LibraryVariant]).
 * 2. [Variant.buildType] is `release` (i.e. not `debug`, or some custom value).
 *
 * @returns the receiver [SetProperty] of [AndroidVariantInfo]s.
 */
internal fun SetProperty<AndroidVariantInfo>.collectFrom(
    androidComponents: AndroidComponentsExtension<*, *, *>,
): SetProperty<AndroidVariantInfo> {
    androidComponents.selector()

    androidComponents.onVariants { variant ->
        variant.components
            .filterIsInstance<Variant>()
            .forEach { component ->
                add(
                    AndroidVariantInfo(
                        name = component.name,
                        isPublishable = variant.buildType.equals("release", ignoreCase = true),
                        compileClasspath = component.compileClasspath,
                        kotlinSources = component.sources.kotlin?.all,
                        javaSources = component.sources.java?.all,
                    )
                )
            }
    }

    return this
}
