package transformers

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.PluginConfigurationImpl
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.model.dfs
import org.jetbrains.dokka.pages.ClasslikePageNode
import org.jetbrains.dokka.pages.ContentHeader
import org.jetbrains.dokka.pages.ContentNode
import org.jetbrains.dokka.pages.ContentText
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.jetbrains.kotlin.utils.addIfNotNull
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DivisionSwitchTest : AbstractCoreTest() {

    private val query = """
            |/src/source0.kt
            package package0
            /** 
            * Documentation for ClassA 
            */
            class ClassA {
                val A: String = "A"
                fun a() {}
                fun b() {}
            }
            
            /src/source1.kt
            package package0
            /**
            * Documentation for ClassB
            */
            class ClassB : ClassA() {
                val B: String = "B"
                fun d() {}
                fun e() {}
            }
        """.trimMargin()

    private fun configuration(switchOn: Boolean) = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
            }
        }
        pluginsConfigurations.addIfNotNull(
            PluginConfigurationImpl(
                DokkaBase::class.qualifiedName!!,
                DokkaConfiguration.SerializationFormat.JSON,
                """{ "separateInheritedMembers": $switchOn }""",
            )
        )
    }

    private fun testClassB(switchOn: Boolean, operation: (ClasslikePageNode) -> Unit) {
        testInline(
            query,
            configuration(switchOn),
            cleanupOutput = true
        ) {
            pagesTransformationStage = { root ->
                val classB = root.dfs { it.name == "ClassB" } as? ClasslikePageNode
                assertNotNull(classB, "Tested class not found!")
                operation(classB)
            }
        }
    }

    private fun ClasslikePageNode.findSectionWithName(name: String) : ContentNode? {
        var sectionHeader: ContentHeader? = null
        return content.dfs { node ->
            node.children.filterIsInstance<ContentHeader>().any { header ->
                header.children.firstOrNull { it is ContentText && it.text == name }?.also { sectionHeader = header } != null
            }
        }?.children?.dropWhile { child -> child != sectionHeader  }?.drop(1)?.firstOrNull()
    }

    @Test
    fun `should not split inherited and regular methods`() {
        testClassB(false) { classB ->
            val functions = classB.findSectionWithName("Functions")
            assertNotNull(functions, "Functions not found!")
            assertEquals(7, functions.children.size, "Incorrect number of functions found")
        }
    }

    @Test
    fun `should not split inherited and regular properties`() {
        testClassB(false) { classB ->
            val properties = classB.findSectionWithName("Properties")
            assertNotNull(properties, "Properties not found!")
            assertEquals(2, properties.children.size, "Incorrect number of properties found")
        }
    }

    @Test
    fun `should split inherited and regular methods`() {
        testClassB(true) { classB ->
            val functions = classB.findSectionWithName("Functions")
            val inheritedFunctions = classB.findSectionWithName("Inherited functions")
            assertNotNull(functions, "Functions not found!")
            assertEquals(2, functions.children.size, "Incorrect number of functions found")
            assertNotNull(inheritedFunctions, "Inherited functions not found!")
            assertEquals(5, inheritedFunctions.children.size, "Incorrect number of inherited functions found")
        }
    }

    @Test
    fun `should split inherited and regular properties`() {
        testClassB(true) { classB ->
            val properties = classB.findSectionWithName("Properties")
            assertNotNull(properties, "Properties not found!")
            assertEquals(1, properties.children.size, "Incorrect number of properties found")
            val inheritedProperties = classB.findSectionWithName("Inherited properties")
            assertNotNull(inheritedProperties, "Inherited properties not found!")
            assertEquals(1, inheritedProperties.children.size, "Incorrect number of inherited properties found")
        }
    }
}