package org.jetbrains.dokka.pagesSerialization

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.pagesSerialization.renderers.PagesSerializationRenderer
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.querySingle

class PagesSerializationPlugin : DokkaPlugin() {
    val dokkaBasePlugin by lazy { plugin<DokkaBase>() }

    val pagesRenderer by extending {
        (CoreExtensions.renderer
                providing {
            PagesSerializationRenderer(
                dokkaBasePlugin.querySingle { outputWriter },
                dokkaBasePlugin.querySingle { locationProviderFactory })
        }
                override dokkaBasePlugin.htmlRenderer)
    }
}