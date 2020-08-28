package org.jetbrains.dokka.analysis

import org.jetbrains.dokka.plugability.DokkaPlugin

class KotlinAnlysisPlugin: DokkaPlugin() {
    val kotlinAnalysis by extensionPoint<KotlinAnalysis>()

    val defaultKotlinAnalysis by extending {
        kotlinAnalysis providing ::KotlinAnalysis
    }
}