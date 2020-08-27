package parsers

import org.jetbrains.dokka.base.parsers.moduleAndPackage.*
import org.jetbrains.dokka.base.parsers.moduleAndPackage.ModuleAndPackageDocumentation.Classifier.Module
import org.jetbrains.dokka.base.parsers.moduleAndPackage.ModuleAndPackageDocumentation.Classifier.Package
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class ParseModuleAndPackageDocumentationFragmentsTest {

    @Test
    fun `basic example`() {

        val source = source(
            """
                # Module kotlin-demo
                Module description
        
                # Package org.jetbrains.kotlin.demo
                Package demo description
                ## Level 2 heading
                Heading 2
        
                # Package org.jetbrains.kotlin.demo2
                Package demo2 description
                """.trimIndent()
        )
        val fragments = parseModuleAndPackageDocumentationFragments(source)

        assertEquals(
            listOf(
                ModuleAndPackageDocumentationFragment(
                    classifier = Module,
                    name = "kotlin-demo",
                    documentation = "Module description",
                    source = source
                ),
                ModuleAndPackageDocumentationFragment(
                    classifier = Package,
                    name = "org.jetbrains.kotlin.demo",
                    documentation = "Package demo description\n## Level 2 heading\nHeading 2",
                    source = source
                ),
                ModuleAndPackageDocumentationFragment(
                    classifier = Package,
                    name = "org.jetbrains.kotlin.demo2",
                    documentation = "Package demo2 description",
                    source = source
                )
            ),
            fragments
        )
    }

    @Test
    fun `no module name specified fails`() {
        val exception = assertThrows<IllegalModuleAndPackageDocumentation> {
            parseModuleAndPackageDocumentationFragments(
                source(
                    """
                    # Module
                    No module name given
                    """.trimIndent()
                )
            )
        }

        assertTrue(
            "Missing Module name" in exception.message.orEmpty(),
            "Expected 'Missing Module name' in error message"
        )
    }

    @Test
    fun `no package name specified does not fail`() {
        val source = source(
            """
            # Package
            This is a root package
            """.trimIndent()
        )
        val fragments = parseModuleAndPackageDocumentationFragments(source)
        assertEquals(1, fragments.size, "Expected a single package fragment")

        assertEquals(
            ModuleAndPackageDocumentationFragment(
                name = "",
                classifier = Package,
                documentation = "This is a root package",
                source = source
            ),
            fragments.single()
        )
    }

    @Test
    fun `white space in module name is supported`() {
        val fragment = parseModuleAndPackageDocumentationFragments(
            source(
                """
                # Module My Module 
                Documentation for my module
                """.trimIndent()
            )
        )

        assertEquals(
            Module, fragment.single().classifier,
            "Expected module being parsec"
        )

        assertEquals(
            "My Module", fragment.single().name,
            "Expected module name with white spaces being parsed"
        )

        assertEquals(
            "Documentation for my module", fragment.single().documentation,
            "Expected documentation being available"
        )
    }

    @Test
    fun `white space in package name fails`() {
        val exception = assertThrows<IllegalModuleAndPackageDocumentation> {
            parseModuleAndPackageDocumentationFragments(
                source(
                    """
                    # Package my package
                    """.trimIndent()
                )
            )
        }

        assertTrue(
            "Package my package" in exception.message.orEmpty(),
            "Expected problematic statement in error message"
        )
    }

    @Test
    fun `multiple whitespaces are supported in first line`() {
        val source = source(
            """
            #    Module my-module
            My Module
            #   Package com.my.package
            My Package
                """.trimIndent()
        )
        val fragments = parseModuleAndPackageDocumentationFragments(source)

        assertEquals(
            listOf(
                ModuleAndPackageDocumentationFragment(
                    classifier = Module,
                    name = "my-module",
                    documentation = "My Module",
                    source = source
                ),
                ModuleAndPackageDocumentationFragment(
                    classifier = Package,
                    name = "com.my.package",
                    documentation = "My Package",
                    source = source
                )
            ),
            fragments
        )
    }

    @Test
    fun `parse from file`(@TempDir temporaryFolder: Path) {
        val file = temporaryFolder.resolve("other.md").toFile()
        file.writeText(
            """
                # Module MyModule
                D1
                # Package com.sample
                D2
            """.trimIndent()
        )

        assertEquals(
            listOf(
                ModuleAndPackageDocumentationFragment(
                    classifier = Module,
                    name = "MyModule",
                    documentation = "D1",
                    source = ModuleAndPackageDocumentationFile(file)
                ),
                ModuleAndPackageDocumentationFragment(
                    classifier = Package,
                    name = "com.sample",
                    documentation = "D2",
                    source = ModuleAndPackageDocumentationFile(file)
                )
            ),
            parseModuleAndPackageDocumentationFragments(file)
        )
    }


    private fun source(documentation: String): ModuleAndPackageDocumentationSource =
        object : ModuleAndPackageDocumentationSource() {
            override val sourceDescription: String = "inline test"
            override val documentation: String = documentation
        }
}
