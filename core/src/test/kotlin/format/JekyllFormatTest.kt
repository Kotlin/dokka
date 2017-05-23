package format

import org.jetbrains.dokka.DocumentationModule
import org.jetbrains.dokka.DocumentationNode
import org.jetbrains.dokka.JekyllFormatService
import org.jetbrains.dokka.KotlinLanguageService
import org.jetbrains.dokka.tests.InMemoryLocationService
import org.jetbrains.dokka.tests.tempLocation
import org.jetbrains.dokka.tests.verifyOutput
import org.junit.Test

class JekyllFormatTest {
    private val jekyllService = JekyllFormatService(InMemoryLocationService, KotlinLanguageService())

    @Test fun linkFormat() {
        verifyJekyllNode("linkFormat")
    }

    private fun verifyJekyllNode(fileName: String, withKotlinRuntime: Boolean = false) {
        verifyJekyllNodes(fileName, withKotlinRuntime) { model -> model.members.single().members }
    }

    private fun verifyJekyllNodes(fileName: String, withKotlinRuntime: Boolean = false, nodeFilter: (DocumentationModule) -> List<DocumentationNode>) {
        verifyOutput("testdata/format/jekyll/$fileName.kt", ".md", withKotlinRuntime = withKotlinRuntime) { model, output ->
            jekyllService.createOutputBuilder(output, tempLocation).appendNodes(nodeFilter(model))
        }
    }
}