package transformers

import org.jetbrains.dokka.base.transformers.documentables.ModuleAndPackageDocumentationReader
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.utilities.DokkaConsoleLogger
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import testApi.testRunner.dokkaConfiguration
import testApi.testRunner.sourceSet


class ContextModuleAndPackageDocumentationReaderTest2: AbstractContextModuleAndPackageDocumentationReaderTest() {

    private val include by lazy { temporaryDirectory.resolve("include.md").toFile() }

    @BeforeEach
    fun materializeInclude() {
        include.writeText(
            """
            # Module MyModuleDisplayName
            Matching: moduleDisplayName
            
            # Module myModuleName
            Matching: moduleName
            """.trimIndent()
        )
    }

    private val sourceSet by lazy {
        sourceSet {
            moduleName = "myModuleName"
            moduleDisplayName = "MyModuleDisplayName"
            includes = listOf(include.canonicalPath)
        }
    }

    private val context by lazy {
        DokkaContext.create(
            configuration = dokkaConfiguration {
                sourceSets {
                    add(sourceSet)
                }
            },
            logger = DokkaConsoleLogger,
            pluginOverrides = emptyList()
        )
    }

    private val reader by lazy { ModuleAndPackageDocumentationReader(context) }


    @Test
    fun `module matches for moduleName and moduleDisplayName`() {
        val documentation = reader[DModule("myModuleName", sourceSets = setOf(sourceSet))]
        assertEquals(1, documentation.keys.size, "Expected only one entry from sourceSet")
        assertEquals(sourceSet, documentation.keys.single(), "Expected only one entry from sourceSet")
        assertEquals(
            listOf("Matching: moduleDisplayName", "Matching: moduleName"), documentation.texts
        )
    }
}
