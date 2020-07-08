package javadoc.transformers.documentables

import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.javadoc.JavadocPlugin
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.junit.jupiter.api.Test
import utils.*
import java.nio.file.Paths

class JavadocDocumentableSourceSetFilterTransformerTest : AbstractCoreTest() {
    @Test
    fun `sourceset filter should filter all content from non-jvm sources`(){
        val testDataDir = getTestDataDir("basicMultiplatformTest").toAbsolutePath()

        val configuration = dokkaConfiguration {
            format = "javadoc"
            sourceSets {
                sourceSet {
                    moduleName = "example"
                    analysisPlatform = "js"
                    sourceRoots = listOf("jsMain", "commonMain").map {
                        Paths.get("$testDataDir/$it/kotlin").toString()
                    }
                    name = "js"
                }
                sourceSet {
                    moduleName = "example"
                    analysisPlatform = "jvm"
                    sourceRoots = listOf("jvmMain", "commonMain").map {
                        Paths.get("$testDataDir/$it/kotlin").toString()
                    }
                    name = "jvm"
                }
                sourceSet {
                    moduleName = "example"
                    analysisPlatform = "common"
                    sourceRoots = listOf("commonMain").map {
                        Paths.get("$testDataDir/$it/kotlin").toString()
                    }
                    name = "common"
                }
            }
        }

        testFromData(
            configuration,
            pluginOverrides = listOf(JavadocPlugin())
        ){
            documentablesTransformationStage = { module ->
                val classlikes = module.packages.flatMap { it.classlikes }
                val properties = classlikes.flatMap { it.properties }
                val functions = classlikes.flatMap { it.functions }

                assert(module.hasOnlyJvmSourcesets())

                assert(module.packages.size == 1)
                module.packages.forEach { assert(it.hasOnlyJvmSourcesets()) }

                classlikes.forEach { assert(it.hasOnlyJvmSourcesets()) }
                properties.forEach { assert(it.hasOnlyJvmSourcesets()) }
                functions.forEach { assert(it.hasOnlyJvmSourcesets()) }
            }
        }
    }

    private fun Documentable.hasOnlyJvmSourcesets(): Boolean = sourceSets.all { it.analysisPlatform == Platform.jvm }
}