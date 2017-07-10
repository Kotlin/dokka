package org.jetbrains.dokka.tests.asciidoctor

import Formats.multipleOption
import Formats.parseOptions
import Formats.singleOption
import org.junit.Test
import kotlin.test.assertEquals

/**
 * @author Mario Toffia
 */
class AsciidoctorOptionsTest {
    @Test fun asciidocOptionsGetParsedCorrectly() {
        val options = """
								--base-dir the-base-dir
								--attribute
								"project_name=project-name"
								--attribute
								"project_version=1.0.2-SNAPSHOT"
								--attribute
								"project_desc=the-project-description"
								--attribute
								"imagesdir=asciidoctor.configuration.images-dir"
								--attribute
								"data-uri=asciidoctor.configuration.images-dir"
								--gem-path
								project.build.directory/gems-provided
								--require
								asciidoctor-diagram
"""

        val opts = parseOptions(options)

        assertEquals("the-base-dir", opts.singleOption("base-dir"))
        assertEquals(5, opts.multipleOption("attribute").size)
        assertEquals("asciidoctor.configuration.images-dir", opts.singleOption("attribute", "imagesdir"))
        assertEquals("asciidoctor.configuration.images-dir", opts.singleOption("attribute", "data-uri"))
        assertEquals("1.0.2-SNAPSHOT", opts.singleOption("attribute", "project_version"))
        assertEquals("asciidoctor-diagram", opts.singleOption("require"))
        assertEquals("", opts.singleOption("kalle-kobra"))
    }
}