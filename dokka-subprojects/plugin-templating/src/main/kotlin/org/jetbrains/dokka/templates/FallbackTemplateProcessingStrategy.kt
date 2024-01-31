/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.templates

import org.jetbrains.dokka.DokkaConfiguration
import java.io.File

public class FallbackTemplateProcessingStrategy : TemplateProcessingStrategy {

    override fun process(input: File, output: File, moduleContext: DokkaConfiguration.DokkaModuleDescription?): Boolean {
        if (input != output) input.copyTo(output, overwrite = true)
        return true
    }
}
