package org.jetbrains.dokka.kotlinAsJava

import org.jetbrains.dokka.kotlinAsJava.converters.asJava
import org.jetbrains.dokka.kotlinAsJava.transformers.JvmNameDocumentableTransformer
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DPackage
import org.jetbrains.dokka.model.DProperty
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.utilities.DokkaLogger

private val JVM_NAME_DOCUMENTABLE_TRANSFORMER by lazy {
    JvmNameDocumentableTransformer()
}

fun DPackage.transformToJava(logger: DokkaLogger): DPackage {
    return JVM_NAME_DOCUMENTABLE_TRANSFORMER.transform(this.asJava(), logger)
}

fun DClasslike.transformToJava(logger: DokkaLogger): DClasslike {
    return JVM_NAME_DOCUMENTABLE_TRANSFORMER.transform(this.asJava(), logger)
}

fun DFunction.transformToJava(logger: DokkaLogger, containingClassName: String, isTopLevel: Boolean = false): List<DFunction> {
    return this.asJava(containingClassName, isTopLevel).map { JVM_NAME_DOCUMENTABLE_TRANSFORMER.transform(it, logger) }
}

fun DProperty.transformToJava(logger: DokkaLogger, isTopLevel: Boolean = false, relocateToClass: String? = null): DProperty {
    return JVM_NAME_DOCUMENTABLE_TRANSFORMER.transform(this.asJava(isTopLevel, relocateToClass), logger)
}
