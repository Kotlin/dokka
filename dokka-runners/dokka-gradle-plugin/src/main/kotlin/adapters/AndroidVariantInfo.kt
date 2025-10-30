/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.adapters

import com.android.build.api.variant.AndroidComponentsExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.SetProperty
import org.jetbrains.dokka.gradle.internal.findExtensionLenient

/**
 * Store details about a [com.android.build.api.variant.Variant].
 *
 * @param[name] [com.android.build.api.variant.Variant.name].
 * @param[hasPublishedComponent] `true` if any component of the variant is 'published',
 * i.e. it is an instance of [com.android.build.api.variant.Variant].
 */
internal data class AndroidVariantInfo(
    val name: String,
    val hasPublishedComponent: Boolean,
    val compileClasspath: FileCollection,
    val compileConfiguration: Configuration,
)


/** Try and get [AndroidComponentsExtension], or `null` if it's not present. */
internal fun Project.findAndroidComponentExtension(): AndroidComponentsExtension<*, *, *>? =
    try {
        findExtensionLenient<AndroidComponentsExtension<*, *, *>>("androidComponents")
    } catch (_: NoClassDefFoundError) {
        null
    } catch (_: ClassNotFoundException) {
        null
    }


/**
 * Collect [AndroidVariantInfo]s for all variants in the project.
 *
 * Should only be called when AGP is applied (otherwise the `androidComponents` extension will be missing).
 */
internal fun collectAndroidVariants(
    androidComponents: AndroidComponentsExtension<*, *, *>,
    androidVariants: SetProperty<AndroidVariantInfo>,
) {
    androidComponents.onVariants { variant ->
        val hasPublishedComponent =
            variant.components.any { component ->
                component is com.android.build.api.variant.Variant
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
