package linking

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.doc.DocumentationLink
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import org.junit.jupiter.api.Assertions.assertEquals
import java.lang.AssertionError

class EnumValuesLinking : BaseAbstractTest() {

    @Test
    fun `check if enum values are correctly linked`() {
        val testDataDir = getTestDataDir("linking").toAbsolutePath()
        testFromData(
            dokkaConfiguration {
                sourceSets {
                    sourceSet {
                        sourceRoots = listOf(Paths.get("$testDataDir/jvmMain/kotlin").toString())
                        analysisPlatform = "jvm"
                        name = "jvm"
                    }
                }
            }
        ) {
            documentablesTransformationStage = {
                val classlikes = it.packages.single().children
                assertEquals(4, classlikes.size)

                val javaLinker = classlikes.single { it.name == "JavaLinker" }
                javaLinker.documentation.values.single().children.run {
                    when (val kotlinLink = this[0].children[1].children[1]) {
                        is DocumentationLink -> kotlinLink.dri.run {
                            assertEquals("KotlinEnum", this.classNames)
                            assertEquals("ON_CREATE", this.callable?.name)
                        }
                        else -> throw AssertionError("Link node is not DocumentationLink type")
                    }

                    when (val javaLink = this[0].children[2].children[1]) {
                        is DocumentationLink -> javaLink.dri.run {
                            assertEquals("JavaEnum", this.classNames)
                            assertEquals("ON_DECEIT", this.callable?.name)
                        }
                        else -> throw AssertionError("Link node is not DocumentationLink type")
                    }
                }

                val kotlinLinker = classlikes.single { it.name == "KotlinLinker" }
                kotlinLinker.documentation.values.single().children.run {
                    when (val kotlinLink = this[0].children[0].children[5]) {
                        is DocumentationLink -> kotlinLink.dri.run {
                            assertEquals("KotlinEnum", this.classNames)
                            assertEquals("ON_CREATE", this.callable?.name)
                        }
                        else -> throw AssertionError("Link node is not DocumentationLink type")
                    }

                    when (val javaLink = this[0].children[0].children[9]) {
                        is DocumentationLink -> javaLink.dri.run {
                            assertEquals("JavaEnum", this.classNames)
                            assertEquals("ON_DECEIT", this.callable?.name)
                        }
                        else -> throw AssertionError("Link node is not DocumentationLink type")
                    }
                }
            }
        }
    }
}
