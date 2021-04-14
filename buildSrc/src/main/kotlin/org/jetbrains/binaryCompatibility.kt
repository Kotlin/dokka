package org.jetbrains

import kotlinx.validation.ApiValidationExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

internal object BinaryCompatibilityConfig {
    val ignoredPublications = setOf("kotlinAnalysisIntelliJ", "kotlinAnalysis", "kotlinAnalysisCompiler")
    val ignoredSubprojects = setOf(
        "search-component",
        "compiler-dependency",
        "intellij-dependency",
        "kotlin-analysis",
        "frontend"
    )
}

internal fun Project.registerBinaryCompatibilityCheck(publicationName: String) {
    publicationName.takeIf {
        it !in BinaryCompatibilityConfig.ignoredPublications
    }?.let {
        if (tasks.findByName("apiBuild") == null) {
            plugins.apply(kotlinx.validation.BinaryCompatibilityValidatorPlugin::class.java)
            configure<ApiValidationExtension> {
                ignoredProjects.addAll(
                    BinaryCompatibilityConfig.ignoredSubprojects.intersect(allprojects.map { it.name })
                )
            }
        }
    }
}