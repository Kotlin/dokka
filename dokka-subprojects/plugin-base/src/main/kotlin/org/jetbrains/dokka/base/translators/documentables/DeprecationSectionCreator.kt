/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.translators.documentables

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.base.signatures.KotlinSignatureUtils.annotations
import org.jetbrains.dokka.base.transformers.documentables.isDeprecated
import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder.DocumentableContentBuilder
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.pages.ContentKind
import org.jetbrains.dokka.pages.ContentStyle
import org.jetbrains.dokka.pages.TextStyle

/**
 * Main header for [Deprecated] section
 */
private const val DEPRECATED_HEADER_LEVEL = 3

/**
 * Header for a direct parameter of [Deprecated] annotation,
 * such as [Deprecated.message] and [Deprecated.replaceWith]
 */
private const val DIRECT_PARAM_HEADER_LEVEL = 4

internal fun PageContentBuilder.DocumentableContentBuilder.deprecatedSectionContent(
    documentable: Documentable,
    platforms: Set<DokkaConfiguration.DokkaSourceSet>
) {
    val allAnnotations = documentable.annotations()
    if (allAnnotations.isEmpty()) {
        return
    }

    platforms.forEach { platform ->
        val platformAnnotations = allAnnotations[platform] ?: emptyList()
        val deprecatedPlatformAnnotations = platformAnnotations.filter { it.isDeprecated() }

        if (deprecatedPlatformAnnotations.isNotEmpty()) {
            group(kind = ContentKind.Deprecation, sourceSets = setOf(platform), styles = emptySet()) {
                val kotlinAnnotation = deprecatedPlatformAnnotations.find { it.dri.packageName == "kotlin" }
                val javaAnnotation = deprecatedPlatformAnnotations.find { it.dri.packageName == "java.lang" }

                // If both annotations are present, priority is given to Kotlin's annotation since it
                // contains more useful information, and Java's annotation is probably there
                // for interop with Java callers, so it should be OK to ignore it
                if (kotlinAnnotation != null) {
                    createKotlinDeprecatedSectionContent(kotlinAnnotation, platformAnnotations)
                } else if (javaAnnotation != null) {
                    createJavaDeprecatedSectionContent(javaAnnotation)
                }
            }
        }
    }
}

/**
 * @see [DeprecatedSinceKotlin]
 */
private fun findDeprecatedSinceKotlinAnnotation(annotations: List<Annotations.Annotation>): Annotations.Annotation? {
    return annotations.firstOrNull {
        it.dri.packageName == "kotlin" && it.dri.classNames == "DeprecatedSinceKotlin"
    }
}

/**
 * Section with details for Kotlin's [kotlin.Deprecated] annotation
 */
private fun DocumentableContentBuilder.createKotlinDeprecatedSectionContent(
    deprecatedAnnotation: Annotations.Annotation,
    allAnnotations: List<Annotations.Annotation>
) {
    val deprecatedSinceKotlinAnnotation = findDeprecatedSinceKotlinAnnotation(allAnnotations)
    header(
        level = DEPRECATED_HEADER_LEVEL,
        text = createKotlinDeprecatedHeaderText(deprecatedAnnotation, deprecatedSinceKotlinAnnotation)
    )

    deprecatedSinceKotlinAnnotation?.let {
        createDeprecatedSinceKotlinFootnoteContent(it)
    }

    deprecatedAnnotation.takeStringParam("message")?.let {
        group(styles = setOf(TextStyle.Paragraph)) {
            text(it)
        }
    }

    createReplaceWithSectionContent(deprecatedAnnotation)
}

private fun createKotlinDeprecatedHeaderText(
    kotlinDeprecatedAnnotation: Annotations.Annotation,
    deprecatedSinceKotlinAnnotation: Annotations.Annotation?
): String {
    if (deprecatedSinceKotlinAnnotation != null) {
        // In this case there's no single level, it's dynamic based on api version,
        // so there should be a footnote with levels and their respective versions
        return "Deprecated"
    }

    val deprecationLevel = kotlinDeprecatedAnnotation.params["level"]?.let { (it as? EnumValue)?.enumName }
    return when (deprecationLevel) {
        "DeprecationLevel.ERROR" -> "Deprecated (with error)"
        "DeprecationLevel.HIDDEN" -> "Deprecated (hidden)"
        else -> "Deprecated"
    }
}

/**
 * Footnote for [DeprecatedSinceKotlin] annotation used in stdlib
 *
 * Notice that values are empty by default, so it's not guaranteed that all three will be set
 */
private fun DocumentableContentBuilder.createDeprecatedSinceKotlinFootnoteContent(
    deprecatedSinceKotlinAnnotation: Annotations.Annotation
) {
    group(styles = setOf(ContentStyle.Footnote)) {
        deprecatedSinceKotlinAnnotation.takeStringParam("warningSince")?.let {
            group(styles = setOf(TextStyle.Paragraph)) {
                text("Warning since $it")
            }
        }
        deprecatedSinceKotlinAnnotation.takeStringParam("errorSince")?.let {
            group(styles = setOf(TextStyle.Paragraph)) {
                text("Error since $it")
            }
        }
        deprecatedSinceKotlinAnnotation.takeStringParam("hiddenSince")?.let {
            group(styles = setOf(TextStyle.Paragraph)) {
                text("Hidden since $it")
            }
        }
    }
}

/**
 * Section for [ReplaceWith] parameter of [kotlin.Deprecated] annotation
 */
private fun DocumentableContentBuilder.createReplaceWithSectionContent(kotlinDeprecatedAnnotation: Annotations.Annotation) {
    val replaceWithAnnotation = (kotlinDeprecatedAnnotation.params["replaceWith"] as? AnnotationValue)?.annotation
        ?: return

    header(
        level = DIRECT_PARAM_HEADER_LEVEL,
        text = "Replace with"
    )

    // Signature: vararg val imports: String
    val imports = (replaceWithAnnotation.params["imports"] as? ArrayValue)
        ?.value
        ?.mapNotNull { (it as? StringValue)?.value }
        ?: emptyList()

    if (imports.isNotEmpty()) {
        codeBlock(language = "kotlin", styles = setOf(TextStyle.Monospace)) {
            imports.forEach {
                text("import $it")
                breakLine()
            }
        }
    }

    replaceWithAnnotation.takeStringParam("expression")?.removeSurrounding("`")?.let {
        codeBlock(language = "kotlin", styles = setOf(TextStyle.Monospace)) {
            text(it)
        }
    }
}

/**
 * Section with details for Java's [java.lang.Deprecated] annotation
 */
private fun DocumentableContentBuilder.createJavaDeprecatedSectionContent(
    deprecatedAnnotation: Annotations.Annotation,
) {
    val isForRemoval = deprecatedAnnotation.takeBooleanParam("forRemoval", default = false)
    header(
        level = DEPRECATED_HEADER_LEVEL,
        text = if (isForRemoval) "Deprecated (for removal)" else "Deprecated"
    )
    deprecatedAnnotation.takeStringParam("since")?.let {
        group(styles = setOf(ContentStyle.Footnote)) {
            text("Since version $it")
        }
    }
}

private fun Annotations.Annotation.takeBooleanParam(name: String, default: Boolean): Boolean =
    (this.params[name] as? BooleanValue)?.value ?: default

private fun Annotations.Annotation.takeStringParam(name: String): String? =
    (this.params[name] as? StringValue)?.takeIf { it.value.isNotEmpty() }?.value
