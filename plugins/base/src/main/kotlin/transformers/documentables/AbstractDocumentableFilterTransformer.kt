package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.model.*

interface AbstractDocumentableFilterTransformer {
    fun <T> alwaysTrue(a: T, p: DokkaConfiguration.DokkaSourceSet) = true
    fun <T> alwaysFalse(a: T, p: DokkaConfiguration.DokkaSourceSet) = false

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
        val filteredPackages = packages.map {
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
        functions: List<DFunction>,
        additionalCondition: (DFunction, DokkaConfiguration.DokkaSourceSet) -> Boolean = ::alwaysTrue
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

    fun <T : Documentable> List<T>.transform(
        additionalCondition: (T, DokkaConfiguration.DokkaSourceSet) -> Boolean = ::alwaysTrue,
        alternativeCondition: (T, DokkaConfiguration.DokkaSourceSet) -> Boolean = ::alwaysFalse,
        recreate: (T, Set<DokkaConfiguration.DokkaSourceSet>) -> T
    ): Pair<Boolean, List<T>>

    fun filterProperties(
        properties: List<DProperty>,
        additionalCondition: (DProperty, DokkaConfiguration.DokkaSourceSet) -> Boolean = ::alwaysTrue
    ): Pair<Boolean, List<DProperty>> =
        properties.transform(additionalCondition) { original, filteredPlatforms ->
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

    private fun filterEnumEntries(entries: List<DEnumEntry>, filteredPlatforms: Set<DokkaConfiguration.DokkaSourceSet>) =
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

    fun <T : Documentable> T.filterPlatforms(
        additionalCondition: (T, DokkaConfiguration.DokkaSourceSet) -> Boolean = ::alwaysTrue,
        alternativeCondition: (T, DokkaConfiguration.DokkaSourceSet) -> Boolean = ::alwaysFalse
    ): Set<DokkaConfiguration.DokkaSourceSet>

    private fun filterClasslikes(
        classlikeList: List<DClasslike>,
        additionalCondition: (DClasslike, DokkaConfiguration.DokkaSourceSet) -> Boolean = ::alwaysTrue
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