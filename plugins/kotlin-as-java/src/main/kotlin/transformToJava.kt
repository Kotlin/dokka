package org.jetbrains.dokka.kotlinAsJava

import org.jetbrains.dokka.kotlinAsJava.converters.KotlinToJavaConverter
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
    with(KotlinToJavaConverter(context)) {
        return JVM_NAME_DOCUMENTABLE_TRANSFORMER.transform(this@transformToJava.asJava(), context)
    }
}

fun DClasslike.transformToJava(context: DokkaContext): DClasslike {
    with(KotlinToJavaConverter(context)) {
        return JVM_NAME_DOCUMENTABLE_TRANSFORMER.transform(this@transformToJava.asJava(), context)
    }
}

fun DFunction.transformToJava(context: DokkaContext, containingClassName: String, isTopLevel: Boolean = false): List<DFunction> {
    with(KotlinToJavaConverter(context)) {
        return this@transformToJava.asJava(containingClassName, isTopLevel)
            .map { JVM_NAME_DOCUMENTABLE_TRANSFORMER.transform(it, context) }
    }
}

fun DProperty.transformToJava(context: DokkaContext, isTopLevel: Boolean = false, relocateToClass: String? = null): DProperty {
    with(KotlinToJavaConverter(context)) {
        return JVM_NAME_DOCUMENTABLE_TRANSFORMER.transform(this@transformToJava.asJava(isTopLevel, relocateToClass), context)
    }
}
