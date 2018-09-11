package org.jetbrains.dokka.tests

import org.jetbrains.dokka.*
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
        val byLocations = nodes.groupBy { location(it) }
        byLocations.forEach { (loc, _) ->
            if (byLocations.size > 1) {
                if (sb.isNotBlank() && !sb.endsWith('\n')) {
                    sb.appendln()
                }
                sb.appendln("<!-- File: ${loc.file.relativeTo(root).toUnixString()} -->")
            }
            sb.append(loc.file.readText())
        }
    }
}