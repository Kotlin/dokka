/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.adapters

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.Variant
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.SetProperty
import org.jetbrains.dokka.gradle.internal.findExtensionLenient
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

/**
 * Store details about a [Variant].
 *
 * @param[name] [Variant.name].
 * @param[hasPublishedComponent] `true` if any component of the variant is 'published',
 * i.e. it is an instance of [Variant].
 */
internal data class AndroidVariantInfo(
    val name: String,
    val hasPublishedComponent: Boolean,
    val compileClasspath: FileCollection,
    val compileConfiguration: Configuration,
)


/** Try and get [AndroidComponentsExtension], or `null` if it's not present. */
internal fun Project.findAndroidComponentsExtension(): AndroidComponentsExtension<*, *, *>? =
    try {
        findExtensionLenient<AndroidComponentsExtension<*, *, *>>("androidComponents")
    } catch (_: NoClassDefFoundError) {
        null
    } catch (_: ClassNotFoundException) {
        null
    }


/**
 * Collect [AndroidVariantInfo]s of the Android [Variant]s in this Android project.
 *
 * We store the collected data in a custom class to aid with Configuration Cache compatibility.
 *
 * This function must only be called when AGP is applied
 * (otherwise [findAndroidComponentsExtension] will return `null`),
 * i.e. inside a `withPlugin(...) {}` block.
 *
 * ## How to determine publishability of AGP Variants
 *
 * There are several Android Gradle plugins.
 * Each AGP has a specific associated [Variant]:
 * - `com.android.application` - [com.android.build.api.variant.ApplicationVariant]
 * - `com.android.library` - [com.android.build.api.variant.DynamicFeatureVariant]
 * - `com.android.test` - [com.android.build.api.variant.LibraryVariant]
 * - `com.android.dynamic-feature` - [com.android.build.api.variant.TestVariant]
 *
 * A [Variant] is 'published' (or otherwise shared with other projects).
 * Note that a [Variant] might have [nestedComponents][Variant.nestedComponents].
 * If any of these [com.android.build.api.variant.Component]s are [Variant]s,
 * then the [Variant] itself should be considered 'publishable'.
 *
 * If a [KotlinSourceSet] has an associated [Variant],
 * it should therefore be documented by Dokka by default.
 *
 * ### Associating Variants with Compilations with SourceSets
 *
 * So, how can we associate a [KotlinSourceSet] with a [Variant]?
 *
 * Fortunately, Dokka already knows about the [KotlinCompilation]s associated with a specific [KotlinSourceSet].
 *
 * So, for each [KotlinCompilation], find a [Variant] with the same name,
 * i.e. [KotlinCompilation.getName] is the same as [Variant.name].
 *
 * Next, determine if the [Variant] associated with a [KotlinCompilation] is 'publishable' by
 * checking if it _or_ any of its [nestedComponents][Variant.nestedComponents]
 * are 'publishable' (i.e. is an instance of [Variant]).
 * (We can we use [Variant.components] to check both the [Variant] and its `nestedComponents` the same time.)
 */
internal fun collectAndroidVariants(
    androidComponents: AndroidComponentsExtension<*, *, *>,
    androidVariants: SetProperty<AndroidVariantInfo>,
) {
    androidComponents.onVariants { variant ->
        val hasPublishedComponent =
            variant.components.any { component ->
                // a Variant is a subtype of a Component that is shared with consumers,
                // so Dokka should consider it 'publishable'
                component is Variant
            }

        androidVariants.add(
            AndroidVariantInfo(
                name = variant.name,
                hasPublishedComponent = hasPublishedComponent,
                compileClasspath = variant.compileClasspath,
                compileConfiguration = variant.compileConfiguration,
            )
        )
    }
}
