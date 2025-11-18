/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.engine.plugins

import kotlinx.serialization.json.buildJsonObject
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.jetbrains.dokka.gradle.internal.InternalDokkaGradlePluginApi
import org.jetbrains.dokka.gradle.internal.putIfNotNull
import javax.inject.Inject

/**
 * Configuration for Dokka Kotlin Playground Samples Plugin.
 *
 * The Kotlin Playground Samples Plugin makes `@sample` code blocks interactive and runnable using [Kotlin Playground](https://github.com/JetBrains/kotlin-playground).
 *
 * Note: The Kotlin Playground Samples Plugin only works with Dokka's HTML format.
 */
abstract class DokkaKotlinPlaygroundSamplesParameters
@InternalDokkaGradlePluginApi
@Inject
constructor(
    name: String
) : DokkaPluginParametersBaseSpec(
    name,
    DOKKA_KOTLIN_PLAYGROUND_SAMPLES_PLUGIN_FQN
) {

    /**
     * URL to the Kotlin Playground JS script.
     */
    @get:Input
    @get:Optional
    abstract val kotlinPlaygroundScript: Property<String>

    /**
     * URL to the Kotlin Playground server for running and compiling samples. Used by the Kotlin Playground script.
     */
    @get:Input
    @get:Optional
    abstract val kotlinPlaygroundServer: Property<String>

    override fun jsonEncode(): String {
        return buildJsonObject {
            putIfNotNull("kotlinPlaygroundScript", kotlinPlaygroundScript.orNull)
            putIfNotNull("kotlinPlaygroundServer", kotlinPlaygroundServer.orNull)
        }.toString()
    }

    companion object {
        const val DOKKA_KOTLIN_PLAYGROUND_SAMPLES_PLUGIN_PARAMETERS_NAME = "kotlinPlaygroundSamples"
        const val DOKKA_KOTLIN_PLAYGROUND_SAMPLES_PLUGIN_FQN =
            "org.jetbrains.dokka.kotlinPlaygroundSamples.KotlinPlaygroundSamplesPlugin"
    }
}
