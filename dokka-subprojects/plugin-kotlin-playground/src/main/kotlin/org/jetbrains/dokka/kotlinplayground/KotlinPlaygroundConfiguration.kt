/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.kotlinplayground

import org.jetbrains.dokka.plugability.ConfigurableBlock

/**
 * Configuration for the Kotlin Playground plugin.
 * 
 * Allows customization of the Kotlin Playground integration for rendering runnable code samples.
 */
public data class KotlinPlaygroundConfiguration(
    /**
     * URL to the Kotlin Playground script. 
     * 
     * If not specified, defaults to the official Kotlin Playground script.
     * Users can provide their own playground script URL for custom setups.
     */
    public val playgroundScript: String? = null,
    
    /**
     * Base URL for the Kotlin Playground server.
     * 
     * If specified and using a custom playground setup, this can point to a custom
     * playground server instead of the default play.kotlinlang.org.
     * 
     * Note: This requires a custom playground script that supports the custom server URL.
     */
    public val playgroundServerUrl: String? = null
) : ConfigurableBlock