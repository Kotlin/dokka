package org.jetbrains.dokka.kotlinAsJava.transformers

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.documentation.DocumentableTransformer

class JvmNameDocumentableTransformer : DocumentableTransformer {
    private val jvmNameProvider = JvmNameProvider()
    private lateinit var context: DokkaContext

    override fun invoke(original: DModule, context: DokkaContext): DModule {
        this.context = context
        return original.copy(packages = original.packages.map { transform(it) })
    }

    fun <T : Documentable> transform(documentable: T): T =
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
                        dri = transformCallable(documentable.dri, name),
                        name = name
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

    fun transformClassLike(documentable: DClasslike): DClasslike =
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

    fun transformGetterAndSetter(entry: DProperty): DProperty =
        with(entry) {
            copy(
                setter = setter?.let { setter ->
                    val setterName = jvmNameProvider.nameAsJavaSetter(this)
                    setter.copy(
                        dri = transformCallable(setter.dri, setterName),
                        name = setterName
                    )
                },
                getter = getter?.let { getter ->
                    val getterName = jvmNameProvider.nameAsJavaGetter(this)
                    getter.copy(
                        dri = transformCallable(getter.dri, getterName),
                        name = getterName
                    )
                })
        }


    fun transformCallable(dri: DRI, callableName: String): DRI =
        dri.copy(callable = dri.callable?.copy(name = callableName))

}