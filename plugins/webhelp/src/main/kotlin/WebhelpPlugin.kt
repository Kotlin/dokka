package org.jetbrains.dokka.webhelp

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.webhelp.location.WebhelpLocationProviderFactory
import org.jetbrains.dokka.webhelp.renderers.WebhelpRenderer

class WebhelpPlugin : DokkaPlugin() {
    val dokkaBasePlugin by lazy { plugin<DokkaBase>() }

    val webhelpRenderer by extending {
        CoreExtensions.renderer providing ::WebhelpRenderer override dokkaBasePlugin.htmlRenderer
    }

    val webhelpLocationProviderFactory by extending {
        dokkaBasePlugin.locationProviderFactory providing ::WebhelpLocationProviderFactory override dokkaBasePlugin.locationProvider
    }
}