package org.jetbrains.dokka.tests.format

import org.jetbrains.dokka.ContentBlock
import org.jetbrains.dokka.ContentText
import org.jetbrains.dokka.DokkaConsoleLogger
import org.jetbrains.dokka.PackageDocs
import org.junit.Test
import kotlin.test.assertEquals

public class PackageDocsTest {
    @Test fun verifyParse() {
        val docs = PackageDocs(null, DokkaConsoleLogger)
        docs.parse("test/data/packagedocs/stdlib.md", null)
        val packageContent = docs.packageContent["kotlin"]!!
        val block = (packageContent.children.single() as ContentBlock).children.first() as ContentText
        assertEquals("Core functions and types", block.text)
    }
}
