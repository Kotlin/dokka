package org.jetbrains.dokka.kotlinAsJava.transformers

import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.documentation.DocumentableTransformer

class JvmNameDocumentableTransformer : DocumentableTransformer {
    private val jvmNameProvider = JvmNameProvider()

    override fun invoke(original: DModule, context: DokkaContext): DModule {
        return original.copy(packages = original.packages.map { transform(it, context) })
    }

    internal fun <T : Documentable> transform(documentable: T, context: DokkaContext): T {
        val transformResult = with(documentable) {
            when (this) {
                is DPackage -> copy(
                    functions = functions.map { transform(it, context) },
                    properties = properties.map { transform(it, context) },
                    classlikes = classlikes.map { transform(it, context) },
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
                is DClasslike -> transformClassLike(this, context)
                is DEnumEntry -> copy(
                    functions = functions.map { transform(it, context) },
                    properties = properties.map { transform(it, context) },
                    classlikes = classlikes.map { transform(it, context) },
                )
                else -> {
                    context.logger.warn("Failed to translate a JvmName for ${this.javaClass.canonicalName}")
                    this
                }
            }
        }
        @Suppress("UNCHECKED_CAST")
        return transformResult as T
    }

    private fun PropertyContainer<DFunction>.withoutJvmName(): PropertyContainer<DFunction> {
        val annotationsWithoutJvmName = get(Annotations)?.let { annotations ->
            annotations.copy((annotations.directAnnotations).map { (sourceset, annotations) ->
                sourceset to annotations.filterNot { it.isJvmName() }
            }.toMap() + annotations.fileLevelAnnotations)
        }
        val extraWithoutAnnotations: PropertyContainer<DFunction> = minus(Annotations)

        return extraWithoutAnnotations.addAll(listOfNotNull(annotationsWithoutJvmName))
    }

    private fun transformClassLike(documentable: DClasslike, context: DokkaContext): DClasslike =
        with(documentable) {
            when (this) {
                is DClass -> copy(
                    functions = functions.map { transform(it, context) },
                    properties = properties.map { transform(it, context) },
                    classlikes = classlikes.map { transform(it, context) },
                )
                is DAnnotation -> copy(
                    functions = functions.map { transform(it, context) },
                    properties = properties.map { transform(it, context) },
                    classlikes = classlikes.map { transform(it, context) },
                )
                is DObject -> copy(
                    functions = functions.map { transform(it, context) },
                    properties = properties.map { transform(it, context) },
                    classlikes = classlikes.map { transform(it, context) },
                )
                is DEnum -> copy(
                    functions = functions.map { transform(it, context) },
                    properties = properties.map { transform(it, context) },
                    classlikes = classlikes.map { transform(it, context) },
                )
                is DInterface -> copy(
                    functions = functions.map { transform(it, context) },
                    properties = properties.map { transform(it, context) },
                    classlikes = classlikes.map { transform(it, context) },
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
