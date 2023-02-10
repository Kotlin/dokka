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
import org.jetbrains.dokka.javadoc.pages.JavadocFunctionNode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals

class JavadocLocationTest : BaseAbstractTest() {

    @Test
    fun `resolved signature with external links`() {
        val query = """
            |/jvmSrc/javadoc/test/Test.kt
            |package javadoc.test
            |import java.io.Serializable
            |class Test : Serializable, Cloneable {}
        """.trimIndent()

        locationTestInline(query) { rootPageNode, dokkaContext ->
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
        val query = """
            |/jvmSrc/javadoc/test/Test.kt
            |package javadoc.test
            |class Test {
            |   fun test() {}
            |}
        """.trimIndent()

        locationTestInline(query) { rootPageNode, dokkaContext ->
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
        val query = """
            |/jvmSrc/javadoc/test/Test.kt
            |package javadoc.test
            |class Test {
            |   fun test2(s: String) {}
            |}
        """.trimIndent()

        locationTestInline(query) { rootPageNode, dokkaContext ->
            val transformer = htmlTranslator(rootPageNode, dokkaContext)
            val testClassNode = rootPageNode.firstChildOfType<JavadocPackagePageNode> { it.name == "javadoc.test" }
                .firstChildOfType<JavadocClasslikePageNode> { it.name == "Test" }
            val testFunctionNode = testClassNode.methods.first { it.name == "test2" }
            assertEquals(
                """<a href=Test.html#test2(java.lang.String)>test2</a>(<a href=https://docs.oracle.com/javase/8/docs/api/java/lang/String.html>String</a> s)""",
                transformer.htmlForContentNode(
                    testFunctionNode.signature.signatureWithoutModifiers,
                    testClassNode
                )
            )
        }
    }

    @Test
    fun `resolved signature to generic function`() {
        val query = """
            |/jvmSrc/javadoc/test/Test.kt
            |package javadoc.test
            |class Test<A>() {
            |   fun <T> test3(a: A, t: T) {}
            |}
        """.trimIndent()

        locationTestInline(query) { rootPageNode, dokkaContext ->
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
        val query = """
            |/jvmSrc/javadoc/test/Test.kt
            |package javadoc.test
            |class Test {}
        """.trimIndent()

        locationTestInline(query) { rootPageNode, dokkaContext ->
            val locationProvider = dokkaContext.plugin<JavadocPlugin>().querySingle { locationProviderFactory }
                .getLocationProvider(rootPageNode)
            val packageNode = rootPageNode.firstChildOfType<JavadocPackagePageNode> { it.name == "javadoc.test" }
            val packagePath = locationProvider.resolve(packageNode)

            assertEquals("javadoc/test/package-summary", packagePath)
        }
    }

    @Test
    fun `resolve link from another package`(){
        val query = """
            |/jvmSrc/javadoc/test/Test.kt
            |package javadoc.test
            |class Test {}
            |
            |/jvmSrc/another/javadoc/example/Referenced.kt
            |package javadoc.example.another
            |
            |/**
            |  * Referencing element from another package: [javadoc.test.Test]
            | */
            |class Referenced {}
        """.trimIndent()

        locationTestInline(query) { rootPageNode, dokkaContext ->
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

    @Test
    fun `should resolve typealias function parameter`() {
        val query = """
            |/jvmSrc/javadoc/test/FunctionParameters.kt
            |package javadoc.test.functionparams
            |
            |typealias StringTypealias = String
            |
            |class FunctionParameters {
            |    fun withTypealias(typeAliasParam: StringTypealias) {}
            |}
        """.trimIndent()

        locationTestInline(query) { rootPageNode, dokkaContext ->
            val transformer = htmlTranslator(rootPageNode, dokkaContext)
            val methodWithTypealiasParam = rootPageNode.findFunctionNodeWithin(
                packageName = "javadoc.test.functionparams",
                className = "FunctionParameters",
                methodName = "withTypealias"
            )
            val methodSignatureHtml = transformer.htmlForContentNode(methodWithTypealiasParam.signature, null)

            val expectedSignatureHtml = "final <a href=https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html>Unit</a> " +
                    "<a href=javadoc/test/functionparams/FunctionParameters.html#withTypealias(javadoc.test.functionparams.StringTypealias)>withTypealias</a>" +
                    "(<a href=https://docs.oracle.com/javase/8/docs/api/java/lang/String.html>String</a> typeAliasParam)"

            assertEquals(expectedSignatureHtml, methodSignatureHtml)
        }
    }

    @Test
    fun `should resolve definitely non nullable function parameter`() {
        val query = """
            |/jvmSrc/javadoc/test/FunctionParameters.kt
            |package javadoc.test.functionparams
            |
            |class FunctionParameters {
            |    fun <T> withDefinitelyNonNullableType(definitelyNonNullable: T & Any) {}
            |}
        """.trimIndent()

        locationTestInline(query) { rootPageNode, dokkaContext ->
            val transformer = htmlTranslator(rootPageNode, dokkaContext)
            val methodWithVoidParam = rootPageNode.findFunctionNodeWithin(
                packageName = "javadoc.test.functionparams",
                className = "FunctionParameters",
                methodName = "withDefinitelyNonNullableType"
            )
            val methodSignatureHtml = transformer.htmlForContentNode(methodWithVoidParam.signature, null)

            val expectedSignatureHtml = "final &lt;T extends <a href=https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html>Any</a>&gt; " +
                    "<a href=https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html>Unit</a> " +
                    "<a href=javadoc/test/functionparams/FunctionParameters.html#withDefinitelyNonNullableType(T)>withDefinitelyNonNullableType</a>" +
                    "(<a href=javadoc/test/functionparams/FunctionParameters.html#withDefinitelyNonNullableType(T)>T</a> definitelyNonNullable)"

            assertEquals(expectedSignatureHtml, methodSignatureHtml)
        }
    }

    private fun RootPageNode.findFunctionNodeWithin(
        packageName: String,
        className: String,
        methodName: String
    ): JavadocFunctionNode {
        return this
            .firstChildOfType<JavadocPackagePageNode> { it.name == packageName }
            .firstChildOfType<JavadocClasslikePageNode> { it.name == className }
            .methods.single { it.name == methodName }
    }

    private fun locationTestInline(query: String, testHandler: (RootPageNode, DokkaContext) -> Unit) {
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
            query = query,
            configuration = config,
            cleanupOutput = false,
            pluginOverrides = listOf(JavadocPlugin())
        ) { renderingStage = testHandler }
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
