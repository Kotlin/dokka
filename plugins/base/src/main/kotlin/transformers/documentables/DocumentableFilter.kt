package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.DAnnotation
import org.jetbrains.dokka.model.DEnum
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.documentation.PreMergeDocumentableTransformer
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

internal class DocumentableFilter(val context: DokkaContext) : PreMergeDocumentableTransformer {

    override fun invoke(modules: List<DModule>): List<DModule> = modules.map { original ->
        val packageOptions =
            context.configuration.passesConfigurations.first { original.sourceSets.contains(context.sourceSet(it)) }
                .perPackageOptions
        val passOptions = context.configuration.passesConfigurations.first {
            original.sourceSets.contains(context.sourceSet(it))
        }
        original.let {
            DeprecationFilter(passOptions,packageOptions).processModule(it)
        }.let {
            VisibilityFilter(packageOptions, passOptions).processModule(it)
        }.let {
            EmptyPackagesFilter(passOptions).processModule(it)
        }
    }

    private class VisibilityFilter(
        val packageOptions: List<DokkaConfiguration.PackageOptions>,
        val globalOptions: DokkaConfiguration.PassConfiguration
    ) {

        fun Visibility.isAllowedInPackage(packageName: String?) = when (this) {
            is JavaVisibility.Public,
            is JavaVisibility.Default,
            is KotlinVisibility.Public -> true
            else -> packageName != null
                    && packageOptions.firstOrNull { packageName.startsWith(it.prefix) }?.includeNonPublic
                    ?: globalOptions.includeNonPublic
        }

        fun processModule(original: DModule) =
            filterPackages(original.packages).let { (modified, packages) ->
                if (!modified) original
                else
                    DModule(
                        original.name,
                        packages = packages,
                        documentation = original.documentation,
                        sourceSets = original.sourceSets,
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
                    else -> {
                        packagesListChanged = true
                        DPackage(
                            it.dri,
                            functions,
                            properties,
                            classlikes,
                            it.typealiases,
                            it.documentation,
                            it.expectPresentInSet,
                            it.sourceSets,
                            it.extra
                        )
                    }
                }
            }
            return Pair(packagesListChanged, filteredPackages)
        }

        private fun <T : WithVisibility> alwaysTrue(a: T, p: SourceSetData) = true
        private fun <T : WithVisibility> alwaysFalse(a: T, p: SourceSetData) = false

        private fun WithVisibility.visibilityForPlatform(data: SourceSetData): Visibility? = visibility[data]

        private fun <T> T.filterPlatforms(
            additionalCondition: (T, SourceSetData) -> Boolean = ::alwaysTrue,
            alternativeCondition: (T, SourceSetData) -> Boolean = ::alwaysFalse
        ) where T : Documentable, T : WithVisibility =
            sourceSets.filter { d ->
                visibilityForPlatform(d)?.isAllowedInPackage(dri.packageName) == true &&
                        additionalCondition(this, d) ||
                        alternativeCondition(this, d)
            }.toSet()

        private fun <T> List<T>.transform(
            additionalCondition: (T, SourceSetData) -> Boolean = ::alwaysTrue,
            alternativeCondition: (T, SourceSetData) -> Boolean = ::alwaysFalse,
            recreate: (T, Set<SourceSetData>) -> T
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
            additionalCondition: (DFunction, SourceSetData) -> Boolean = ::alwaysTrue
        ) =
            functions.transform(additionalCondition) { original, filteredPlatforms ->
                with(original) {
                    DFunction(
                        dri,
                        name,
                        isConstructor,
                        parameters,
                        documentation.filtered(filteredPlatforms),
                        expectPresentInSet.filtered(filteredPlatforms),
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

        private fun hasVisibleAccessorsForPlatform(property: DProperty, data: SourceSetData) =
            property.getter?.visibilityForPlatform(data)?.isAllowedInPackage(property.dri.packageName) == true ||
                    property.setter?.visibilityForPlatform(data)?.isAllowedInPackage(property.dri.packageName) == true

        private fun filterProperties(
            properties: List<DProperty>,
            additionalCondition: (DProperty, SourceSetData) -> Boolean = ::alwaysTrue
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

        private fun filterEnumEntries(entries: List<DEnumEntry>, filteredPlatforms: Set<SourceSetData>) =
            entries.mapNotNull { entry ->
                if (filteredPlatforms.containsAll(entry.sourceSets)) entry
                else {
                    val intersection = filteredPlatforms.intersect(entry.sourceSets)
                    if (intersection.isEmpty()) null
                    else DEnumEntry(
                        entry.dri,
                        entry.name,
                        entry.documentation.filtered(intersection),
                        entry.expectPresentInSet.filtered(filteredPlatforms),
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
            additionalCondition: (DClasslike, SourceSetData) -> Boolean = ::alwaysTrue
        ): Pair<Boolean, List<DClasslike>> {
            var classlikesListChanged = false
            val filteredClasslikes: List<DClasslike> = classlikeList.mapNotNull {
                with(it) {
                    val filteredPlatforms = filterPlatforms(additionalCondition)
                    if (filteredPlatforms.isEmpty()) {
                        classlikesListChanged = true
                        null
                    } else {
                        var modified = sourceSets.size != filteredPlatforms.size
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
                                expectPresentInSet.filtered(filteredPlatforms),
                                modifier,
                                filteredPlatforms,
                                extra
                            )
                            this is DAnnotation -> DAnnotation(
                                name,
                                dri,
                                documentation.filtered(filteredPlatforms),
                                expectPresentInSet.filtered(filteredPlatforms),
                                sources.filtered(filteredPlatforms),
                                functions,
                                properties,
                                classlikes,
                                visibility.filtered(filteredPlatforms),
                                companion,
                                constructors,
                                generics,
                                filteredPlatforms,
                                extra
                            )
                            this is DEnum -> DEnum(
                                dri,
                                name,
                                enumEntries,
                                documentation.filtered(filteredPlatforms),
                                expectPresentInSet.filtered(filteredPlatforms),
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
                                expectPresentInSet.filtered(filteredPlatforms),
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
                                expectPresentInSet.filtered(filteredPlatforms),
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

    private class DeprecationFilter(
        val globalOptions: DokkaConfiguration.PassConfiguration,
        val packageOptions: List<DokkaConfiguration.PackageOptions>
    ) {

        fun <T> T.isAllowedInPackage(): Boolean where T : WithExtraProperties<T>, T : Documentable {
            val packageName = this.dri.packageName
            val condition = packageName != null && packageOptions.firstOrNull {
                packageName.startsWith(it.prefix)
            }?.skipDeprecated
                    ?: globalOptions.skipDeprecated

            fun T.isDeprecated() = extra[Annotations]?.let { annotations ->
                annotations.content.values.flatten().any {
                    it.dri.toString() == "kotlin/Deprecated///PointingToDeclaration/"
                }
            } ?: false

            return !(condition && this.isDeprecated())
        }

        fun processModule(original: DModule) =
            filterPackages(original.packages).let { (modified, packages) ->
                if (!modified) original
                else
                    DModule(
                        original.name,
                        packages = packages,
                        documentation = original.documentation,
                        sourceSets = original.sourceSets,
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
                    else -> {
                        packagesListChanged = true
                        DPackage(
                            it.dri,
                            functions,
                            properties,
                            classlikes,
                            it.typealiases,
                            it.documentation,
                            it.expectPresentInSet,
                            it.sourceSets,
                            it.extra
                        )
                    }
                }
            }
            return Pair(packagesListChanged, filteredPackages)
        }

        private fun filterFunctions(
            functions: List<DFunction>
        ) = functions.filter { it.isAllowedInPackage() }.let {
            Pair(it.size != functions.size, it)
        }

        private fun filterProperties(
            properties: List<DProperty>
        ): Pair<Boolean, List<DProperty>> = properties.filter {
            it.isAllowedInPackage()
        }.let {
            Pair(properties.size != it.size, it)
        }

        private fun filterEnumEntries(entries: List<DEnumEntry>) =
            entries.filter { it.isAllowedInPackage() }.map { entry ->
                DEnumEntry(
                    entry.dri,
                    entry.name,
                    entry.documentation,
                    entry.expectPresentInSet,
                    filterFunctions(entry.functions).second,
                    filterProperties(entry.properties).second,
                    filterClasslikes(entry.classlikes).second,
                    entry.sourceSets,
                    entry.extra
                )
            }

        private fun filterClasslikes(
            classlikeList: List<DClasslike>
        ): Pair<Boolean, List<DClasslike>> {
            var modified = false
            return classlikeList.filter {
                when (it) {
                    is DClass -> it.isAllowedInPackage()
                    is DInterface -> it.isAllowedInPackage()
                    is DEnum -> it.isAllowedInPackage()
                    is DObject -> it.isAllowedInPackage()
                    is DAnnotation -> it.isAllowedInPackage()
                }
            }.map {
                fun helper(): DClasslike = when (it) {
                    is DClass -> DClass(
                        it.dri,
                        it.name,
                        filterFunctions(it.constructors).let {
                            modified = modified || it.first; it.second
                        },
                        filterFunctions(it.functions).let {
                            modified = modified || it.first; it.second
                        },
                        filterProperties(it.properties).let {
                            modified = modified || it.first; it.second
                        },
                        filterClasslikes(it.classlikes).let {
                            modified = modified || it.first; it.second
                        },
                        it.sources,
                        it.visibility,
                        it.companion,
                        it.generics,
                        it.supertypes,
                        it.documentation,
                        it.expectPresentInSet,
                        it.modifier,
                        it.sourceSets,
                        it.extra
                    )
                    is DAnnotation -> DAnnotation(
                        it.name,
                        it.dri,
                        it.documentation,
                        it.expectPresentInSet,
                        it.sources,
                        filterFunctions(it.functions).let {
                            modified = modified || it.first; it.second
                        },
                        filterProperties(it.properties).let {
                            modified = modified || it.first; it.second
                        },
                        filterClasslikes(it.classlikes).let {
                            modified = modified || it.first; it.second
                        },
                        it.visibility,
                        it.companion,
                        filterFunctions(it.constructors).let {
                            modified = modified || it.first; it.second
                        },
                        it.generics,
                        it.sourceSets,
                        it.extra
                    )
                    is DEnum -> DEnum(
                        it.dri,
                        it.name,
                        filterEnumEntries(it.entries),
                        it.documentation,
                        it.expectPresentInSet,
                        it.sources,
                        filterFunctions(it.functions).let {
                            modified = modified || it.first; it.second
                        },
                        filterProperties(it.properties).let {
                            modified = modified || it.first; it.second
                        },
                        filterClasslikes(it.classlikes).let {
                            modified = modified || it.first; it.second
                        },
                        it.visibility,
                        it.companion,
                        filterFunctions(it.constructors).let {
                            modified = modified || it.first; it.second
                        },
                        it.supertypes,
                        it.sourceSets,
                        it.extra
                    )
                    is DInterface -> DInterface(
                        it.dri,
                        it.name,
                        it.documentation,
                        it.expectPresentInSet,
                        it.sources,
                        filterFunctions(it.functions).let {
                            modified = modified || it.first; it.second
                        },
                        filterProperties(it.properties).let {
                            modified = modified || it.first; it.second
                        },
                        filterClasslikes(it.classlikes).let {
                            modified = modified || it.first; it.second
                        },
                        it.visibility,
                        it.companion,
                        it.generics,
                        it.supertypes,
                        it.sourceSets,
                        it.extra
                    )
                    is DObject -> DObject(
                        it.name,
                        it.dri,
                        it.documentation,
                        it.expectPresentInSet,
                        it.sources,
                        filterFunctions(it.functions).let {
                            modified = modified || it.first; it.second
                        },
                        filterProperties(it.properties).let {
                            modified = modified || it.first; it.second
                        },
                        filterClasslikes(it.classlikes).let {
                            modified = modified || it.first; it.second
                        },
                        it.visibility,
                        it.supertypes,
                        it.sourceSets,
                        it.extra
                    )
                }
                helper()
            }.let {
                Pair(it.size != classlikeList.size || modified, it)
            }
        }
    }

    private class EmptyPackagesFilter(
        val passOptions: DokkaConfiguration.PassConfiguration
    ) {
        fun DPackage.shouldBeSkipped() = passOptions.skipEmptyPackages &&
                functions.isEmpty() &&
                properties.isEmpty() &&
                classlikes.isEmpty()

        fun processModule(module: DModule) = module.copy(
            packages = module.packages.mapNotNull { pckg ->
                if (pckg.shouldBeSkipped()) null
                else pckg
            }
        )
    }
}