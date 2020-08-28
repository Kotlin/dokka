package org.jetbrains.dokka.java.documentables

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.analysis.KotlinAnlysisPlugin
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.querySingle

class JavaDocumetablesPlugin: DokkaPlugin() {
    val psiToDocumentableTranslator by extending {
        CoreExtensions.sourceToDocumentableTranslator providing { ctx ->
            DefaultPsiToDocumentableTranslator(plugin<KotlinAnlysisPlugin>().querySingle{ kotlinAnalysis })
        }
    }
}