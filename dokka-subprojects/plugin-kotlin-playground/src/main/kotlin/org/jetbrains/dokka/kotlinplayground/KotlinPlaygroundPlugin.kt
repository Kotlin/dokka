/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.kotlinplayground

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.plugability.*
import org.jetbrains.dokka.transformers.pages.PageTransformer

/**
 * Plugin that adds Kotlin Playground support for runnable code samples.
 * 
 * When enabled, this plugin transforms code blocks with samples into interactive 
 * Kotlin Playground instances that can be executed in the browser.
 */
public class KotlinPlaygroundPlugin : DokkaPlugin() {

    public val configuration: ExtensionPoint<KotlinPlaygroundConfiguration> by extensionPoint()

    public val playgroundSamplesTransformer: Extension<PageTransformer, *, *> by extending {
        CoreExtensions.pageTransformer providing ::PlaygroundSamplesTransformer order {
            // Run after the default samples transformer but before page merging
            after(plugin<org.jetbrains.dokka.base.DokkaBase>().defaultSamplesTransformer)
        }
    }

    @OptIn(DokkaPluginApiPreview::class)
    override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement =
        PluginApiPreviewAcknowledgement
}