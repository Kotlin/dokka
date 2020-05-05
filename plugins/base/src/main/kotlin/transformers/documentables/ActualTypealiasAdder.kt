package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.documentation.PreMergeDocumentableTransformer

class ActualTypealiasAdder(val context: DokkaContext) : PreMergeDocumentableTransformer {
    override fun invoke(modules: List<DModule>) = modules.map { it.mergeTypealiases() }

    private fun DModule.mergeTypealiases(): DModule = copy(packages = packages.map { pkg ->
        if (pkg.typealiases.isEmpty()) {
            pkg
        } else {
            val typealiases = pkg.typealiases.map { it.dri to it }.toMap()
            pkg.copy(
                classlikes = addActualTypeAliasToClasslikes(pkg.classlikes, typealiases)
            )
        }
    })

    private fun addActualTypeAliasToClasslikes(
        elements: Iterable<DClasslike>,
        typealiases: Map<DRI, DTypeAlias>
    ): List<DClasslike> = elements.flatMap {
        when (it) {
            is DClass -> addActualTypeAlias(
                it.copy(
                    classlikes = addActualTypeAliasToClasslikes(it.classlikes, typealiases)
                ).let(::listOf),
                typealiases
            )
            is DEnum -> addActualTypeAlias(
                it.copy(
                    classlikes = addActualTypeAliasToClasslikes(it.classlikes, typealiases)
                ).let(::listOf),
                typealiases
            )
            is DInterface -> addActualTypeAlias(
                it.copy(
                    classlikes = addActualTypeAliasToClasslikes(it.classlikes, typealiases)
                ).let(::listOf),
                typealiases
            )
            is DObject -> addActualTypeAlias(
                it.copy(
                    classlikes = addActualTypeAliasToClasslikes(it.classlikes, typealiases)
                ).let(::listOf),
                typealiases
            )
            is DAnnotation -> addActualTypeAlias(
                it.copy(
                    classlikes = addActualTypeAliasToClasslikes(it.classlikes, typealiases)
                ).let(::listOf),
                typealiases
            )
            else -> throw IllegalStateException("${it::class.qualifiedName} ${it.name} cannot have extra added")
        } as List<DClasslike>
    }

    private fun <T> addActualTypeAlias(
        elements: Iterable<T>,
        typealiases: Map<DRI, DTypeAlias>
    ): List<T> where T : DClasslike, T : WithExtraProperties<T>, T : WithExpectActual =
        elements.map { element ->
            if (element.expectPresentInSet != null) {
                typealiases[element.dri]?.let { ta ->
                    element.withNewExtras(
                        element.extra + ActualTypealias(
                            mapOf(ta.sourceSets.single() to ta.underlyingType.values.single())
                        )
                    )
                } ?: element
            } else {
                element
            }
        }
}