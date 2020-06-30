package javadoc.location

import javadoc.pages.JavadocClasslikePageNode
import javadoc.pages.JavadocPackagePageNode
import javadoc.renderer.JavadocContentToHtmlTranslator
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.ExternalDocumentationLinkImpl
import org.jetbrains.dokka.javadoc.JavadocPlugin
import org.jetbrains.dokka.model.firstChildOfType
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.junit.jupiter.api.Test

class JavadocTest : AbstractCoreTest() {

    @Test
    fun `resolved signature with external links`() {

        fun externalLink(link: String) = DokkaConfiguration.ExternalDocumentationLink
            .Builder(link)
            .build() as ExternalDocumentationLinkImpl

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
            |/jvmSrc/javadoc/Test.kt
            |package javadoc
            |class Test() : Serializable, Cloneable 
        """.trimIndent(),
            config,
            cleanupOutput = false,
            pluginOverrides = listOf(JavadocPlugin())
        ) {
            renderingStage = { rootPageNode, dokkaContext ->
                val transformer = JavadocContentToHtmlTranslator(
                    dokkaContext.plugin<JavadocPlugin>().querySingle { locationProviderFactory }
                        .getLocationProvider(rootPageNode),
                    dokkaContext
                )
                val testClass = rootPageNode.firstChildOfType<JavadocPackagePageNode>()
                    .firstChildOfType<JavadocClasslikePageNode>()
                assert(
                    "public final class <a href=javadoc/Test.html>Test</a> extends <a href=https://docs.oracle.com/javase/8/docs/api/java/lang/Cloneable.html>Cloneable</a>"
                    == transformer.htmlForContentNode(testClass.signature, null)
                )
            }
        }
    }
}
