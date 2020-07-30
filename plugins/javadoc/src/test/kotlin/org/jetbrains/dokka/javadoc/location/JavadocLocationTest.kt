package org.jetbrains.dokka.javadoc.location

import org.jetbrains.dokka.javadoc.pages.JavadocClasslikePageNode
import org.jetbrains.dokka.javadoc.pages.JavadocPackagePageNode
import org.jetbrains.dokka.javadoc.renderer.JavadocContentToHtmlTranslator
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.ExternalDocumentationLink
import org.jetbrains.dokka.ExternalDocumentationLinkImpl
import org.jetbrains.dokka.javadoc.JavadocPlugin
import org.jetbrains.dokka.model.firstChildOfType
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals

class JavadocLocationTest : AbstractCoreTest() {

    private fun locationTestInline(testHandler: (RootPageNode, DokkaContext) -> Unit) {
        fun externalLink(link: String) = ExternalDocumentationLink(link)

        val config = dokkaConfiguration {
            format = "javadoc"
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("jvmSrc/")
                    externalDocumentationLinks = listOf(
                        externalLink("https://docs.oracle.com/javase/8/docs/api/"),
                        externalLink("https://kotlinlang.org/api/latest/jvm/stdlib/")
                    )
                    analysisPlatform = "jvm"
                }
            }
        }
        testInline(
            """
            |/jvmSrc/javadoc/test/Test.kt
            |package javadoc.test
            |import java.io.Serializable
            |class Test<A>() : Serializable, Cloneable {
            |   fun test() {}
            |   fun test2(s: String) {}
            |   fun <T> test3(a: A, t: T) {}
            |}
        """.trimIndent(),
            config,
            cleanupOutput = false,
            pluginOverrides = listOf(JavadocPlugin())
        ) { renderingStage = testHandler }
    }

    @Test
    fun `resolved signature with external links`() {

        locationTestInline { rootPageNode, dokkaContext ->
            val transformer = htmlTranslator(rootPageNode, dokkaContext)
            val testClass = rootPageNode.firstChildOfType<JavadocPackagePageNode>()
                .firstChildOfType<JavadocClasslikePageNode>()
            assertEquals(
                " implements <a href=https://docs.oracle.com/javase/8/docs/api/java/io/Serializable.html>Serializable</a>, <a href=https://docs.oracle.com/javase/8/docs/api/java/lang/Cloneable.html>Cloneable</a>",
                transformer.htmlForContentNode(testClass.signature.supertypes!!, null)
            )
        }
    }

    @Test
    fun `resolved signature to no argument function`() {

        locationTestInline { rootPageNode, dokkaContext ->
            val transformer = htmlTranslator(rootPageNode, dokkaContext)
            val testClassNode = rootPageNode.firstChildOfType<JavadocPackagePageNode>()
                .firstChildOfType<JavadocClasslikePageNode> { it.name == "Test" }
            val testFunctionNode = testClassNode.methods.first { it.name == "test" }
            assertEquals(
                """<a href=Test.html#test()>test</a>()""",
                transformer.htmlForContentNode(
                    testFunctionNode.signature.signatureWithoutModifiers,
                    testClassNode
                )
            )
        }
    }

    @Test
    fun `resolved signature to one argument function`() {

        locationTestInline { rootPageNode, dokkaContext ->
            val transformer = htmlTranslator(rootPageNode, dokkaContext)
            val testClassNode = rootPageNode.firstChildOfType<JavadocPackagePageNode>()
                .firstChildOfType<JavadocClasslikePageNode> { it.name == "Test" }
            val testFunctionNode = testClassNode.methods.first { it.name == "test2" }
            assertEquals(
                """<a href=Test.html#test2(String)>test2</a>(<a href=https://docs.oracle.com/javase/8/docs/api/java/lang/String.html>String</a> s)""",
                transformer.htmlForContentNode(
                    testFunctionNode.signature.signatureWithoutModifiers,
                    testClassNode
                )
            )
        }
    }

    @Test
    fun `resolved signature to generic function`() {

        locationTestInline { rootPageNode, dokkaContext ->
            val transformer = htmlTranslator(rootPageNode, dokkaContext)
            val testClassNode = rootPageNode.firstChildOfType<JavadocPackagePageNode>()
                .firstChildOfType<JavadocClasslikePageNode> { it.name == "Test" }
            val testFunctionNode = testClassNode.methods.first { it.name == "test3" }
            assertEquals(
                """<a href=Test.html#test3(A,T)>test3</a>(<a href=Test.html>A</a> a, <a href=Test.html#test3(A,T)>T</a> t)""",
                transformer.htmlForContentNode(
                    testFunctionNode.signature.signatureWithoutModifiers,
                    testClassNode
                )
            )
        }
    }

    @Test
    fun `resolved package path`() {

        locationTestInline { rootPageNode, dokkaContext ->
            val locationProvider = dokkaContext.plugin<JavadocPlugin>().querySingle { locationProviderFactory }
                .getLocationProvider(rootPageNode)
            val packageNode = rootPageNode.firstChildOfType<JavadocPackagePageNode>()
            val packagePath = locationProvider.resolve(packageNode)

            assertEquals("javadoc/test/package-summary", packagePath)
        }
    }

    private fun htmlTranslator(rootPageNode: RootPageNode, dokkaContext: DokkaContext): JavadocContentToHtmlTranslator {
        val locationProvider = dokkaContext.plugin<JavadocPlugin>().querySingle { locationProviderFactory }
            .getLocationProvider(rootPageNode) as JavadocLocationProvider
        return htmlTranslator(dokkaContext, locationProvider)
    }

    private fun htmlTranslator(
        dokkaContext: DokkaContext,
        locationProvider: JavadocLocationProvider
    ) = JavadocContentToHtmlTranslator(locationProvider, dokkaContext)
}
