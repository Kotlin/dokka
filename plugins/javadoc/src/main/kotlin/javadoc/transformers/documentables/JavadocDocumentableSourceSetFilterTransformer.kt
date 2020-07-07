package javadoc.transformers.documentables

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.base.transformers.documentables.AbstractDocumentableFilterTransformer
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.WithVisibility
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.documentation.PreMergeDocumentableTransformer
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class JavadocDocumentableSourceSetFilterTransformer : PreMergeDocumentableTransformer {
    private val List<Documentable>.jvmSourceSets
        get() = flatMap { it.sourceSets }.filter { it.analysisPlatform == Platform.jvm }

    override fun invoke(modules: List<DModule>): List<DModule> =
        modules.jvmSourceSets.firstOrNull()?.let {
            jvm ->
                val filter = DocumentableSourceSetFilter(jvm)
                modules.map { filter.processModule(it) }
        } ?: modules

    private inner class DocumentableSourceSetFilter(
        val desiredSourceSet: DokkaConfiguration.DokkaSourceSet
    ): AbstractDocumentableFilterTransformer {

        override fun <T: Documentable> List<T>.transform(
            additionalCondition: (T, DokkaConfiguration.DokkaSourceSet) -> Boolean,
            alternativeCondition: (T, DokkaConfiguration.DokkaSourceSet) -> Boolean,
            recreate: (T, Set<DokkaConfiguration.DokkaSourceSet>) -> T
        ): Pair<Boolean, List<T>> {
            var changed = false
            val values = mapNotNull { t ->
                val filteredPlatforms = t.filterPlatforms(additionalCondition, alternativeCondition)
                when (filteredPlatforms.size) {
                    t.sourceSets.size -> t
                    0 -> {
                        changed = true
                        null
                    }
                    else -> {
                        changed = true
                        recreate(t, filteredPlatforms)
                    }
                }
            }
            return Pair(changed, values)
        }

        override fun <T : Documentable> T.filterPlatforms(
            additionalCondition: (T, DokkaConfiguration.DokkaSourceSet) -> Boolean,
            alternativeCondition: (T, DokkaConfiguration.DokkaSourceSet) -> Boolean
        ): Set<DokkaConfiguration.DokkaSourceSet> =
            sourceSets.filter { sourceSet ->
                sourceSet == desiredSourceSet && additionalCondition(this, sourceSet) ||
                        alternativeCondition(this, sourceSet)
            }.toSet()
    }
}