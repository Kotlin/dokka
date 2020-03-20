package javadoc

import org.jetbrains.dokka.javadoc.JavadocPlugin
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.junit.jupiter.api.Test

class JavadocTest : AbstractCoreTest() {

    @Test
    fun test() {
        val config = dokkaConfiguration {
            format = "javadoc"
            passes {
                pass {
                    sourceRoots = listOf("jvmSrc/")
                    analysisPlatform = "jvm"
                    targets = listOf("jvm")
                }
            }
        }

        testInline("""
            |/jvmSrc/javadoc/Test.kt
            |package javadoc
            |class Test()
        """.trimIndent(),
            config,
            cleanupOutput = false,
            pluginOverrides = listOf(JavadocPlugin())
            ) {
            pagesTransformationStage = {
                it
            }
        }
    }
}