package org.jetbrains.dokka.javadoc.validity

import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.validity.PreGenerationChecker
import org.jetbrains.dokka.validity.PreGenerationCheckerOutput

class MultiplatformConfiguredChecker(val context: DokkaContext) : PreGenerationChecker {

    override fun invoke(): PreGenerationCheckerOutput {
        val isSinglePlatform = context.configuration.sourceSets.all { sourceSet ->
            val platform = sourceSet.analysisPlatform
            (platform == Platform.jvm || platform == Platform.common)
        }
        return PreGenerationCheckerOutput(isSinglePlatform, listOfNotNull(errorMessage.takeUnless { isSinglePlatform }))
    }

    companion object {
        const val errorMessage =
            "Dokka Javadoc plugin currently does not support generating documentation for multiplatform project. Please, adjust your configuration"
    }
}