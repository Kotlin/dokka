/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.javadoc.validity

import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.validity.PreGenerationChecker
import org.jetbrains.dokka.validity.PreGenerationCheckerOutput

public class MultiplatformConfiguredChecker(
    public val context: DokkaContext
) : PreGenerationChecker {

    override fun invoke(): PreGenerationCheckerOutput {
        val isSinglePlatform = context.configuration.sourceSets.all { sourceSet ->
            val platform = sourceSet.analysisPlatform
            (platform == Platform.jvm || platform == Platform.common)
        }
        return PreGenerationCheckerOutput(isSinglePlatform, listOfNotNull(errorMessage.takeUnless { isSinglePlatform }))
    }

    public companion object {
        public const val errorMessage: String =
            "Dokka Javadoc plugin currently does not support generating documentation for multiplatform project. Please, adjust your configuration"
    }
}
