/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.dokkatoo.dokka.parameters.builders

import org.jetbrains.dokka.DokkaModuleDescriptionImpl
import org.jetbrains.dokka.DokkaSourceSetImpl
import org.jetbrains.dokka.dokkatoo.dokka.parameters.DokkaModuleDescriptionSpec
import org.jetbrains.dokka.dokkatoo.internal.DokkatooInternalApi
import java.io.File

/**
 * Convert the Gradle-focused [DokkaModuleDescriptionSpec] into a [DokkaSourceSetImpl] instance,
 * which will be passed to Dokka Generator.
 *
 * The conversion is defined in a separate class to try and prevent classes from Dokka Generator
 * leaking into the public API.
 */
// to be used to fix https://github.com/adamko-dev/dokkatoo/issues/67
@DokkatooInternalApi
internal object DokkaModuleDescriptionBuilder {

    fun build(
        spec: DokkaModuleDescriptionSpec,
        includes: Set<File>,
        sourceOutputDirectory: File,
    ): DokkaModuleDescriptionImpl =
        DokkaModuleDescriptionImpl(
            name = spec.name,
            relativePathToOutputDirectory = File(
                spec.projectPath.get().removePrefix(":").replace(':', '/')
            ),
            includes = includes,
            sourceOutputDirectory = sourceOutputDirectory,
        )
}
