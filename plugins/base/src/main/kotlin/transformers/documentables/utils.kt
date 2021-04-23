package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.links.PointingToDeclaration
import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.ExceptionInSupertypes
import org.jetbrains.dokka.model.properties.WithExtraProperties

fun <T> T.isDeprecated() where T : WithExtraProperties<out Documentable> =
    deprecatedAnnotation != null

fun <T> T.hasOneOfAnnotations(fqNames: Set<String>) where T : WithExtraProperties<out Documentable> =
    extra[Annotations]?.let { annotations ->
        (annotations.directAnnotations + annotations.fileLevelAnnotations).values.flatten().any {
            if (it.dri.target != PointingToDeclaration) return@any false
            if (it.dri.classNames.isNullOrEmpty()) return@any false

            val fqName = buildString {
                if (!it.dri.packageName.isNullOrEmpty())
                    append(it.dri.packageName!!.trim('.'))
                append(".${it.dri.classNames}")
            }

            fqName in fqNames
        }
    } ?: false

val <T> T.deprecatedAnnotation where T : WithExtraProperties<out Documentable>
    get() = extra[Annotations]?.let { annotations ->
        annotations.directAnnotations.values.flatten().firstOrNull {
            it.dri.toString() == "kotlin/Deprecated///PointingToDeclaration/" ||
                    it.dri.toString() == "java.lang/Deprecated///PointingToDeclaration/"
        }
    }

val <T : WithExtraProperties<out Documentable>> T.isException: Boolean
    get() = extra[ExceptionInSupertypes] != null