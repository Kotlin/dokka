package org.jetbrains.dokka.tests.format

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import org.jetbrains.dokka.*
import org.jetbrains.dokka.tests.assertEqualsIgnoringSeparators
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreProjectEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File

class PackageDocsTest {

    private lateinit var testDisposable: Disposable

    @Before
    fun setup() {
        testDisposable = Disposer.newDisposable()
    }

    @After
    fun cleanup() {
        Disposer.dispose(testDisposable)
    }

    fun createPackageDocs(linkResolver: DeclarationLinkResolver?): PackageDocs {
        val environment = KotlinCoreEnvironment.createForTests(testDisposable, CompilerConfiguration.EMPTY, EnvironmentConfigFiles.JVM_CONFIG_FILES)
        return PackageDocs(linkResolver, DokkaConsoleLogger, environment, mock(), mock())
    }

    @Test fun verifyParse() {

        val docs = createPackageDocs(null)
        docs.parse("testdata/packagedocs/stdlib.md", emptyList())
        val packageContent = docs.packageContent["kotlin"]!!
        val block = (packageContent.children.single() as ContentBlock).children.first() as ContentText
        assertEquals("Core functions and types", block.text)
    }

    @Test fun testReferenceLinksInPackageDocs() {
        val mockLinkResolver = mock<DeclarationLinkResolver> {
            val exampleCom = "http://example.com"
            on { tryResolveContentLink(any(), eq(exampleCom)) } doAnswer { ContentExternalLink(exampleCom) }
        }

        val mockPackageDescriptor = mock<PackageFragmentDescriptor> {}

        val docs = createPackageDocs(mockLinkResolver)
        docs.parse("testdata/packagedocs/referenceLinks.md", listOf(mockPackageDescriptor))

        checkMarkdownOutput(docs, "testdata/packagedocs/referenceLinks")
    }

    fun checkMarkdownOutput(docs: PackageDocs, expectedFilePrefix: String) {

        val generator = FileGenerator(File(""))

        val out = StringBuilder()
        val outputBuilder = MarkdownOutputBuilder(
                out,
                FileLocation(generator.root),
                generator,
                KotlinLanguageService(),
                ".md",
                emptyList()
        )
        fun checkOutput(content: Content, filePostfix: String) {
            outputBuilder.appendContent(content)
            val expectedFile = File(expectedFilePrefix + filePostfix)
            assertEqualsIgnoringSeparators(expectedFile, out.toString())
            out.setLength(0)
        }

        checkOutput(docs.moduleContent, ".module.md")

        docs.packageContent.forEach {
            (name, content) ->
            checkOutput(content, ".$name.md")
        }

    }
}
