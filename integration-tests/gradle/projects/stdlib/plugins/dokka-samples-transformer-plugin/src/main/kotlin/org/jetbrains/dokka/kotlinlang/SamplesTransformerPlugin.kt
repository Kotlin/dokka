package org.jetbrains.dokka.kotlinlang

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.html.DokkaHtml
import org.jetbrains.dokka.plugability.DokkaPlugin

class SamplesTransformerPlugin : DokkaPlugin() {
    private val dokkaHtml by lazy { plugin<DokkaHtml>() }

    val kotlinWebsiteSamplesTransformer by extending {
        CoreExtensions.pageTransformer providing ::KotlinWebsiteSamplesTransformer override dokkaHtml.defaultSamplesTransformer order {
            before(dokkaBase.pageMerger)
        }
    }
}