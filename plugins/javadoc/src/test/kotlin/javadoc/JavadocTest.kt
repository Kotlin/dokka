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

        /*
            |/jvmSrc/javadoc/test/Test2.kt
            |package javadoc.test
            |class Test2()
         */

        testInline("""
            |/jvmSrc/javadoc/Test.kt
            |/**
            |   test
            |**/
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

//    @Test
//    fun test() {
//        val config = dokkaConfiguration {
//            format = "javadoc"
//            passes {
//                pass {
//                    sourceRoots = listOf("main/")
//                    analysisPlatform = "jvm"
//                    targets = listOf("jvm")
//                }
//            }
//        }
//        testFromData(config,
//            cleanupOutput = false,
//            pluginOverrides = listOf(JavadocPlugin())) {
//            pagesTransformationStage = {
//                it
//            }
//        }
//    }
}