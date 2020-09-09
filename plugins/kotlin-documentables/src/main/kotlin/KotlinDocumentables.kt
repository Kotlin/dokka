package org.jetbrains.dokka.kotlin.documentables

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.analysis.KotlinAnlysisPlugin
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.query
import org.jetbrains.dokka.plugability.querySingle

class KotlinDocumentables: DokkaPlugin() {
    val descriptorToDocumentableTranslator by extending {
        CoreExtensions.sourceToDocumentableTranslator providing { ctx ->
            DefaultDescriptorToDocumentableTranslator(plugin<KotlinAnlysisPlugin>().querySingle{ kotlinAnalysis })
        }
    }
}
