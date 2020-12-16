package org.jetbrains.dokka.javadoc.location

import org.jetbrains.dokka.*
import org.jetbrains.dokka.javadoc.pages.JavadocClasslikePageNode
import org.jetbrains.dokka.javadoc.pages.JavadocPackagePageNode
import org.jetbrains.dokka.javadoc.renderer.JavadocContentToHtmlTranslator
import org.jetbrains.dokka.javadoc.JavadocPlugin
import org.jetbrains.dokka.model.firstChildOfType
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals

class JavadocLocationTest : BaseAbstractTest() {

    private fun locationTestInline(testHandler: (RootPageNode, DokkaContext) -> Unit) {
        val config = dokkaConfiguration {
            format = "javadoc"
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("jvmSrc/")
                    externalDocumentationLinks = listOf(
                        DokkaConfiguration.ExternalDocumentationLink.jdk(8),
                        DokkaConfiguration.ExternalDocumentationLink.kotlinStdlib()
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
            |
            |/jvmSrc/another/javadoc/example/Referenced.kt
            |package javadoc.example.another
            |/**
            |  * Referencing element from another package: [javadoc.test.Test]
            | */
            |class Referenced {}
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
            val testClass = rootPageNode.firstChildOfType<JavadocPackagePageNode> { it.name == "javadoc.test" }
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
            val testClassNode = rootPageNode.firstChildOfType<JavadocPackagePageNode> { it.name == "javadoc.test" }
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
            val testClassNode = rootPageNode.firstChildOfType<JavadocPackagePageNode> { it.name == "javadoc.test" }
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
            val testClassNode = rootPageNode.firstChildOfType<JavadocPackagePageNode> { it.name == "javadoc.test" }
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
            val packageNode = rootPageNode.firstChildOfType<JavadocPackagePageNode>() { it.name == "javadoc.test" }
            val packagePath = locationProvider.resolve(packageNode)

            assertEquals("javadoc/test/package-summary", packagePath)
        }
    }

    @Test
    fun `resolve link from another package`(){
        locationTestInline { rootPageNode, dokkaContext ->
            val transformer = htmlTranslator(rootPageNode, dokkaContext)
            val testClassNode = rootPageNode.firstChildOfType<JavadocPackagePageNode> { it.name == "javadoc.example.another" }
                .firstChildOfType<JavadocClasslikePageNode> { it.name == "Referenced" }
            assertEquals(
                """<p>Referencing element from another package: <a href=../../test/Test.html>javadoc.test.Test</a></p>""",
                transformer.htmlForContentNode(
                    testClassNode.description.single(),
                    testClassNode
                )
            )
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
