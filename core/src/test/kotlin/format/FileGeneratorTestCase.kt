package org.jetbrains.dokka.tests

import org.jetbrains.dokka.DocumentationNode
import org.jetbrains.dokka.FileGenerator
import org.jetbrains.dokka.FormatService
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder


abstract class FileGeneratorTestCase {
    abstract val formatService: FormatService

    @get:Rule
    var folder = TemporaryFolder()

    val fileGenerator = FileGenerator(folder.apply { create() }.root)

    @Before
    fun bindGenerator() {
        fileGenerator.formatService = formatService
    }

    fun buildPagesAndReadInto(nodes: List<DocumentationNode>, sb: StringBuilder) = with(fileGenerator) {
        buildPages(nodes)
        nodes.forEach {
            val fileForNode = location(it).file
            sb.append(fileForNode.readText())
        }
    }
}