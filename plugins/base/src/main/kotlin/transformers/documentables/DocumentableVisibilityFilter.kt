package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.DAnnotation
import org.jetbrains.dokka.model.DEnum
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.pages.PlatformData
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.documentation.PreMergeDocumentableTransformer

internal object DocumentableVisibilityFilter : PreMergeDocumentableTransformer {

    override fun invoke(modules: List<DModule>, context: DokkaContext): List<DModule> = modules.map { original ->
        val packageOptions =
            context.configuration.passesConfigurations.first { original.platformData.contains(it.platformData) }
                .perPackageOptions

        DocumentableFilter(packageOptions).processModule(original)
    }

    private class DocumentableFilter(val packageOptions: List<DokkaConfiguration.PackageOptions>) {

        fun Visibility.isAllowedInPackage(packageName: String?) = when (this) {
            is JavaVisibility.Public,
            is JavaVisibility.Default,
            is KotlinVisibility.Public -> true
            else -> packageName != null && packageOptions.firstOrNull { packageName.startsWith(it.prefix) }?.includeNonPublic == true
        }

        fun processModule(original: DModule) =
            filterPackages(original.packages).let { (modified, packages) ->
                if (!modified) original
                else
                    DModule(
                        original.name,
                        packages = packages,
                        documentation = original.documentation,
                        platformData = original.platformData,
                        extra = original.extra
                    )
            }

        private fun filterPackages(packages: List<DPackage>): Pair<Boolean, List<DPackage>> {
            var packagesListChanged = false
            val filteredPackages = packages.mapNotNull {
                var modified = false
                val functions = filterFunctions(it.functions).let { (listModified, list) ->
                    modified = modified || listModified
                    list
                }
                val properties = filterProperties(it.properties).let { (listModified, list) ->
                    modified = modified || listModified
                    list
                }
                val classlikes = filterClasslikes(it.classlikes).let { (listModified, list) ->
                    modified = modified || listModified
                    list
                }
                when {
                    !modified -> it
                    functions.isEmpty() && properties.isEmpty() && classlikes.isEmpty() -> null
                    else -> {
                        packagesListChanged = true
                        DPackage(
                            it.dri,
                            functions,
                            properties,
                            classlikes,
                            it.documentation,
                            it.platformData,
                            it.extra
                        )
                    }
                }
            }
            return Pair(packagesListChanged, filteredPackages)
        }

        private fun <T : WithVisibility> alwaysTrue(a: T, p: PlatformData) = true
        private fun <T : WithVisibility> alwaysFalse(a: T, p: PlatformData) = false

        private fun WithVisibility.visibilityForPlatform(data: PlatformData): Visibility? =
            visibility[data] ?: visibility.expect

        private fun <T> T.filterPlatforms(
            additionalCondition: (T, PlatformData) -> Boolean = ::alwaysTrue,
            alternativeCondition: (T, PlatformData) -> Boolean = ::alwaysFalse
        ) where T : Documentable, T : WithVisibility =
            platformData.filter { d ->
                visibilityForPlatform(d)?.isAllowedInPackage(dri.packageName) == true &&
                        additionalCondition(this, d) ||
                        alternativeCondition(this, d)
            }

        private fun <T> List<T>.transform(
            additionalCondition: (T, PlatformData) -> Boolean = ::alwaysTrue,
            alternativeCondition: (T, PlatformData) -> Boolean = ::alwaysFalse,
            recreate: (T, List<PlatformData>) -> T
        ): Pair<Boolean, List<T>> where T : Documentable, T : WithVisibility {
            var changed = false
            val values = mapNotNull { t ->
                val filteredPlatforms = t.filterPlatforms(additionalCondition, alternativeCondition)
                when (filteredPlatforms.size) {
                    t.visibility.size -> t
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

        private fun filterFunctions(
            functions: List<DFunction>,
            additionalCondition: (DFunction, PlatformData) -> Boolean = ::alwaysTrue
        ) =
            functions.transform(additionalCondition) { original, filteredPlatforms ->
                with(original) {
                    DFunction(
                        dri,
                        name,
                        isConstructor,
                        parameters,
                        documentation.filtered(filteredPlatforms),
                        sources.filtered(filteredPlatforms),
                        visibility.filtered(filteredPlatforms),
                        type,
                        generics.mapNotNull { it.filter(filteredPlatforms) },
                        receiver,
                        modifier,
                        filteredPlatforms,
                        extra
                    )
                }
            }

        private fun hasVisibleAccessorsForPlatform(property: DProperty, data: PlatformData) =
            property.getter?.visibilityForPlatform(data)?.isAllowedInPackage(property.dri.packageName) == true ||
                    property.setter?.visibilityForPlatform(data)?.isAllowedInPackage(property.dri.packageName) == true

        private fun filterProperties(
            properties: List<DProperty>,
            additionalCondition: (DProperty, PlatformData) -> Boolean = ::alwaysTrue
        ): Pair<Boolean, List<DProperty>> =
            properties.transform(additionalCondition, ::hasVisibleAccessorsForPlatform) { original, filteredPlatforms ->
                with(original) {
                    DProperty(
                        dri,
                        name,
                        documentation.filtered(filteredPlatforms),
                        sources.filtered(filteredPlatforms),
                        visibility.filtered(filteredPlatforms),
                        type,
                        receiver,
                        setter,
                        getter,
                        modifier,
                        filteredPlatforms,
                        extra
                    )
                }
            }

        private fun filterEnumEntries(entries: List<DEnumEntry>, filteredPlatforms: List<PlatformData>) =
            entries.mapNotNull { entry ->
                if (filteredPlatforms.containsAll(entry.platformData)) entry
                else {
                    val intersection = filteredPlatforms.intersect(entry.platformData).toList()
                    if (intersection.isEmpty()) null
                    else DEnumEntry(
                        entry.dri,
                        entry.name,
                        entry.documentation.filtered(intersection),
                        filterFunctions(entry.functions) { _, data -> data in intersection }.second,
                        filterProperties(entry.properties) { _, data -> data in intersection }.second,
                        filterClasslikes(entry.classlikes) { _, data -> data in intersection }.second,
                        intersection,
                        entry.extra
                    )
                }
            }

        private fun filterClasslikes(
            classlikeList: List<DClasslike>,
            additionalCondition: (DClasslike, PlatformData) -> Boolean = ::alwaysTrue
        ): Pair<Boolean, List<DClasslike>> {
            var classlikesListChanged = false
            val filteredClasslikes: List<DClasslike> = classlikeList.mapNotNull {
                with(it) {
                    val filteredPlatforms = filterPlatforms(additionalCondition)
                    if (filteredPlatforms.isEmpty()) {
                        classlikesListChanged = true
                        null
                    } else {
                        var modified = platformData.size != filteredPlatforms.size
                        val functions =
                            filterFunctions(functions) { _, data -> data in filteredPlatforms }.let { (listModified, list) ->
                                modified = modified || listModified
                                list
                            }
                        val properties =
                            filterProperties(properties) { _, data -> data in filteredPlatforms }.let { (listModified, list) ->
                                modified = modified || listModified
                                list
                            }
                        val classlikes =
                            filterClasslikes(classlikes) { _, data -> data in filteredPlatforms }.let { (listModified, list) ->
                                modified = modified || listModified
                                list
                            }
                        val companion =
                            if (this is WithCompanion) filterClasslikes(listOfNotNull(companion)) { _, data -> data in filteredPlatforms }.let { (listModified, list) ->
                                modified = modified || listModified
                                list.firstOrNull() as DObject?
                            } else null
                        val constructors = if (this is WithConstructors)
                            filterFunctions(constructors) { _, data -> data in filteredPlatforms }.let { (listModified, list) ->
                                modified = modified || listModified
                                list
                            } else emptyList()
                        val generics =
                            if (this is WithGenerics) generics.mapNotNull { param -> param.filter(filteredPlatforms) } else emptyList()
                        val enumEntries =
                            if (this is DEnum) filterEnumEntries(entries, filteredPlatforms) else emptyList()
                        classlikesListChanged = classlikesListChanged || modified
                        when {
                            !modified -> this
                            this is DClass -> DClass(
                                dri,
                                name,
                                constructors,
                                functions,
                                properties,
                                classlikes,
                                sources.filtered(filteredPlatforms),
                                visibility.filtered(filteredPlatforms),
                                companion,
                                generics,
                                supertypes.filtered(filteredPlatforms),
                                documentation.filtered(filteredPlatforms),
                                modifier,
                                filteredPlatforms,
                                extra
                            )
                            this is DAnnotation -> DAnnotation(
                                name,
                                dri,
                                documentation.filtered(filteredPlatforms),
                                sources.filtered(filteredPlatforms),
                                functions,
                                properties,
                                classlikes,
                                visibility.filtered(filteredPlatforms),
                                companion,
                                constructors,
                                filteredPlatforms,
                                extra
                            )
                            this is DEnum -> DEnum(
                                dri,
                                name,
                                enumEntries,
                                documentation.filtered(filteredPlatforms),
                                sources.filtered(filteredPlatforms),
                                functions,
                                properties,
                                classlikes,
                                visibility.filtered(filteredPlatforms),
                                companion,
                                constructors,
                                supertypes.filtered(filteredPlatforms),
                                filteredPlatforms,
                                extra
                            )
                            this is DInterface -> DInterface(
                                dri,
                                name,
                                documentation.filtered(filteredPlatforms),
                                sources.filtered(filteredPlatforms),
                                functions,
                                properties,
                                classlikes,
                                visibility.filtered(filteredPlatforms),
                                companion,
                                generics,
                                supertypes.filtered(filteredPlatforms),
                                filteredPlatforms,
                                extra
                            )
                            this is DObject -> DObject(
                                name,
                                dri,
                                documentation.filtered(filteredPlatforms),
                                sources.filtered(filteredPlatforms),
                                functions,
                                properties,
                                classlikes,
                                visibility,
                                supertypes.filtered(filteredPlatforms),
                                filteredPlatforms,
                                extra
                            )
                            else -> null
                        }
                    }
                }
            }
            return Pair(classlikesListChanged, filteredClasslikes)
        }
    }
}