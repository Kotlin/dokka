package org.jetbrains.dokka.Formats

import org.jetbrains.dokka.*
import kotlin.reflect.KClass

public interface FormatDescriptor {
    val formatServiceClass: KClass<out FormatService>?
    val outlineServiceClass: KClass<out OutlineFormatService>?
    val generatorServiceClass: KClass<out Generator>
    val packageDocumentationBuilderClass: KClass<out PackageDocumentationBuilder>
    val javaDocumentationBuilderClass: KClass<out JavaDocumentationBuilder>
}
