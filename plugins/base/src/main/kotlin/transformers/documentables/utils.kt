package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.properties.WithExtraProperties

fun <T> T.isDeprecated() where T : WithExtraProperties<out Documentable> =
    deprecatedAnnotation != null

val <T> T.deprecatedAnnotation where T : WithExtraProperties<out Documentable>
    get() = extra[Annotations]?.let { annotations ->
        annotations.content.values.flatten().firstOrNull {
            it.dri.toString() == "kotlin/Deprecated///PointingToDeclaration/" ||
                    it.dri.toString() == "java.lang/Deprecated///PointingToDeclaration/"
        }
    }