package org.jetbrains.dokka.gradle

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation

class ConfigurationExtractor(private val project: Project): AbstractConfigurationExtractor(project) {
     override fun getMainCompilationName(): String = KotlinCompilation.MAIN_COMPILATION_NAME
}