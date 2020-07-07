package javadoc.transformers.documentables

import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.transformers.documentation.PreMergeDocumentableTransformer

class JavadocDocumentableSourceSetFilterTransformer : PreMergeDocumentableTransformer {
    private val List<Documentable>.jvmSourceSets
        get() = flatMap { it.sourceSets }.filter { it.analysisPlatform == Platform.jvm }.distinct()

    override fun invoke(modules: List<DModule>): List<DModule> =
        modules.jvmSourceSets.firstOrNull()?.let { jvm ->
            modules.filter { it.sourceSets.contains(jvm) }
        }.orEmpty()
}