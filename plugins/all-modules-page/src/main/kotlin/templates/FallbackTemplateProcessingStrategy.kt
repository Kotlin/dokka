package org.jetbrains.dokka.allModulesPage.templates

import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.dokka.plugability.DokkaContext
import java.io.File
import java.nio.file.Files

class FallbackTemplateProcessingStrategy(dokkaContext: DokkaContext) : TemplateProcessingStrategy {

    override suspend fun process(input: File, output: File): Boolean  = coroutineScope {
        launch(IO) {
            Files.copy(input.toPath(), output.toPath())
        }
        true
    }
}