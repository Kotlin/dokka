package org.jetbrains.dokka.kotlinAsJava

import org.jetbrains.dokka.kotlinAsJava.converters.asJava
import org.jetbrains.dokka.kotlinAsJava.transformers.JvmNameDocumentableTransformer
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DPackage
import org.jetbrains.dokka.model.DProperty
import org.jetbrains.dokka.plugability.DokkaContext

private val JVM_NAME_DOCUMENTABLE_TRANSFORMER by lazy {
    JvmNameDocumentableTransformer()
}

fun DPackage.transformToJava(context: DokkaContext): DPackage {
    return JVM_NAME_DOCUMENTABLE_TRANSFORMER.transform(this.asJava(), context)
}

fun DClasslike.transformToJava(context: DokkaContext): DClasslike {
    return JVM_NAME_DOCUMENTABLE_TRANSFORMER.transform(this.asJava(), context)
}

fun DFunction.transformToJava(context: DokkaContext, containingClassName: String, isTopLevel: Boolean = false): List<DFunction> {
    return this.asJava(containingClassName, isTopLevel).map { JVM_NAME_DOCUMENTABLE_TRANSFORMER.transform(it, context) }
}

fun DProperty.transformToJava(context: DokkaContext, isTopLevel: Boolean = false, relocateToClass: String? = null): DProperty {
    return JVM_NAME_DOCUMENTABLE_TRANSFORMER.transform(this.asJava(isTopLevel, relocateToClass), context)
}
