package org.jetbrains.dokka.kotlinAsJava.transformers

import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.documentation.DocumentableTransformer

class JvmNameDocumentableTransformer : DocumentableTransformer {
    private val jvmNameProvider = JvmNameProvider()
    private lateinit var context: DokkaContext

    override fun invoke(original: DModule, context: DokkaContext): DModule {
        this.context = context
        return original.copy(packages = original.packages.map { transform(it) })
    }

    private fun <T : Documentable> transform(documentable: T): T =
        with(documentable) {
            when (this) {
                is DPackage -> copy(
                    functions = functions.map { transform(it) },
                    properties = properties.map { transform(it) },
                    classlikes = classlikes.map { transform(it) },
                )
                is DFunction -> {
                    val name = jvmNameProvider.nameFor(this)
                    copy(
                        dri = documentable.dri.withCallableName(name),
                        name = name,
                        extra = extra.withoutJvmName()
                    )
                }
                is DProperty -> transformGetterAndSetter(this)
                is DClasslike -> transformClassLike(this)
                is DEnumEntry -> copy(
                    functions = functions.map { transform(it) },
                    properties = properties.map { transform(it) },
                    classlikes = classlikes.map { transform(it) },
                )
                else -> {
                    context.logger.warn("Failed to translate a JvmName for ${this.javaClass.canonicalName}")
                    this
                }
            }
        } as T

    private fun PropertyContainer<DFunction>.withoutJvmName(): PropertyContainer<DFunction> {
        val annotationsWithoutJvmName = get(Annotations)?.let { annotations ->
            annotations.copy((annotations.directAnnotations).map { (sourceset, annotations) ->
                sourceset to annotations.filterNot { it.isJvmName() }
            }.toMap() + annotations.fileLevelAnnotations)
        }
        val extraWithoutAnnotations: PropertyContainer<DFunction> = minus(Annotations)

        return extraWithoutAnnotations.addAll(listOfNotNull(annotationsWithoutJvmName))
    }

    private fun transformClassLike(documentable: DClasslike): DClasslike =
        with(documentable) {
            when (this) {
                is DClass -> copy(
                    functions = functions.map { transform(it) },
                    properties = properties.map { transform(it) },
                    classlikes = classlikes.map { transform(it) },
                )
                is DAnnotation -> copy(
                    functions = functions.map { transform(it) },
                    properties = properties.map { transform(it) },
                    classlikes = classlikes.map { transform(it) },
                )
                is DObject -> copy(
                    functions = functions.map { transform(it) },
                    properties = properties.map { transform(it) },
                    classlikes = classlikes.map { transform(it) },
                )
                is DEnum -> copy(
                    functions = functions.map { transform(it) },
                    properties = properties.map { transform(it) },
                    classlikes = classlikes.map { transform(it) },
                )
                is DInterface -> copy(
                    functions = functions.map { transform(it) },
                    properties = properties.map { transform(it) },
                    classlikes = classlikes.map { transform(it) },
                )
            }
        }

    private fun transformGetterAndSetter(entry: DProperty): DProperty =
        with(entry) {
            copy(
                setter = jvmNameProvider.nameForSetter(this)?.let { setterName ->
                    setter?.let { setter ->
                        setter.copy(
                            dri = setter.dri.withCallableName(setterName),
                            name = setterName,
                            extra = setter.extra.withoutJvmName()
                        )
                    }
                },
                getter = jvmNameProvider.nameForGetter(this)?.let { getterName ->
                    getter?.let { getter ->
                        getter.copy(
                            dri = getter.dri.withCallableName(getterName),
                            name = getterName,
                            extra = getter.extra.withoutJvmName()
                        )
                    }
                })
        }
}