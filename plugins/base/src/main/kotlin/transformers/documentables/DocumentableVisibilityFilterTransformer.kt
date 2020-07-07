package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.documentation.PreMergeDocumentableTransformer
import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class DocumentableVisibilityFilterTransformer(val context: DokkaContext) : PreMergeDocumentableTransformer {

    override fun invoke(modules: List<DModule>) = modules.map { original ->
        val sourceSet = original.sourceSets.single()
        val packageOptions = sourceSet.perPackageOptions
        DocumentableVisibilityFilter(packageOptions, sourceSet).processModule(original)
    }

    private class DocumentableVisibilityFilter(
        val packageOptions: List<DokkaConfiguration.PackageOptions>,
        val globalOptions: DokkaSourceSet
    ): AbstractDocumentableFilterTransformer {

        fun Visibility.isAllowedInPackage(packageName: String?) = when (this) {
            is JavaVisibility.Public,
            is JavaVisibility.Default,
            is KotlinVisibility.Public -> true
            else -> packageName != null
                    && packageOptions.firstOrNull { packageName.startsWith(it.prefix) }?.includeNonPublic
                    ?: globalOptions.includeNonPublic
        }

        private fun WithVisibility.visibilityForPlatform(data: DokkaSourceSet): Visibility? = visibility[data]

        override fun <T: Documentable> T.filterPlatforms(
            additionalCondition: (T, DokkaSourceSet) -> Boolean,
            alternativeCondition: (T, DokkaSourceSet) -> Boolean
        ): Set<DokkaSourceSet> =
            when (this) {
                is WithVisibility -> filterPlatformsWithVisibility(additionalCondition, alternativeCondition)
                else -> emptySet()
            }

        private fun <T> T.filterPlatformsWithVisibility(
            additionalCondition: (T, DokkaSourceSet) -> Boolean = ::alwaysTrue,
            alternativeCondition: (T, DokkaSourceSet) -> Boolean = ::alwaysFalse
        ) where T : Documentable, T : WithVisibility =
            sourceSets.filter { d ->
                visibilityForPlatform(d)?.isAllowedInPackage(dri.packageName) == true &&
                        additionalCondition(this, d) ||
                        alternativeCondition(this, d)
            }.toSet()

        override fun <T : Documentable> List<T>.transform(
            additionalCondition: (T, DokkaSourceSet) -> Boolean,
            alternativeCondition: (T, DokkaSourceSet) -> Boolean,
            recreate: (T, Set<DokkaSourceSet>) -> T
        ): Pair<Boolean, List<T>> {
            var changed = false
            val values = mapNotNull { t ->
                val filteredPlatforms = t.filterPlatforms(additionalCondition, alternativeCondition)
                when (filteredPlatforms.size) {
                    t.safeAs<WithVisibility>()?.visibility?.size -> t
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

        private fun hasVisibleAccessorsForPlatform(property: DProperty, data: DokkaSourceSet) =
            property.getter?.visibilityForPlatform(data)?.isAllowedInPackage(property.dri.packageName) == true ||
                    property.setter?.visibilityForPlatform(data)?.isAllowedInPackage(property.dri.packageName) == true

        override fun filterProperties(
            properties: List<DProperty>,
            additionalCondition: (DProperty, DokkaSourceSet) -> Boolean
        ): Pair<Boolean, List<DProperty>> =
            properties.transform(additionalCondition, ::hasVisibleAccessorsForPlatform) { original, filteredPlatforms ->
                with(original) {
                    DProperty(
                        dri,
                        name,
                        documentation.filtered(filteredPlatforms),
                        expectPresentInSet.filtered(filteredPlatforms),
                        sources.filtered(filteredPlatforms),
                        visibility.filtered(filteredPlatforms),
                        type,
                        receiver,
                        setter,
                        getter,
                        modifier,
                        filteredPlatforms,
                        generics.mapNotNull { it.filter(filteredPlatforms) },
                        extra
                    )
                }
            }
    }
}
