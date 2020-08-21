package parsers

import org.jetbrains.dokka.base.parsers.IllegalModuleAndPackageDocumentation
import org.jetbrains.dokka.base.parsers.ModuleAndPackageDocFragment
import org.jetbrains.dokka.base.parsers.ModuleAndPackageDocFragment.Classifier.Module
import org.jetbrains.dokka.base.parsers.ModuleAndPackageDocFragment.Classifier.Package
import org.jetbrains.dokka.base.parsers.ModuleAndPackageDocumentationSource
import org.jetbrains.dokka.base.parsers.parseModuleAndPackageDocFragments
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class ParseModuleAndPackageDocFragmentsTest {

    @Test
    fun `basic example`() {

        val fragments = parseModuleAndPackageDocFragments(
            source(
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
        )

        assertEquals(
            listOf(
                ModuleAndPackageDocFragment(
                    classifier = Module,
                    name = "kotlin-demo",
                    documentation = "Module description"
                ),
                ModuleAndPackageDocFragment(
                    classifier = Package,
                    name = "org.jetbrains.kotlin.demo",
                    documentation = "Package demo description\n## Level 2 heading\nHeading 2"
                ),
                ModuleAndPackageDocFragment(
                    classifier = Package,
                    name = "org.jetbrains.kotlin.demo2",
                    documentation = "Package demo2 description"
                )
            ),
            fragments
        )
    }

    @Test
    fun `no module name specified fails`() {
        val exception = assertThrows<IllegalModuleAndPackageDocumentation> {
            parseModuleAndPackageDocFragments(
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
    fun `no package name specified fails`() {
        val exception = assertThrows<IllegalModuleAndPackageDocumentation> {
            parseModuleAndPackageDocFragments(
                source(
                    """
                    # Package
                    No package name given
                    """.trimIndent()
                )
            )
        }

        assertTrue(
            "Missing Package name" in exception.message.orEmpty(),
            "Expected 'Missing Package name' in error message"
        )
    }

    @Test
    fun `white space in module name fails`() {
        val exception = assertThrows<IllegalModuleAndPackageDocumentation> {
            parseModuleAndPackageDocFragments(
                source(
                    """
                    # Module My Module
                    """.trimIndent()
                )
            )
        }

        assertTrue(
            "Module My Module" in exception.message.orEmpty(),
            "Expected problematic statement in error message"
        )
    }

    @Test
    fun `white space in package name fails`() {
        val exception = assertThrows<IllegalModuleAndPackageDocumentation> {
            parseModuleAndPackageDocFragments(
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
        val fragments = parseModuleAndPackageDocFragments(
            source(
                """
                    #    Module my-module
                    My Module
                    #   Package com.my.package
                    My Package
                """.trimIndent()
            )
        )

        assertEquals(
            listOf(
                ModuleAndPackageDocFragment(
                    classifier = Module,
                    name = "my-module",
                    documentation = "My Module"
                ),
                ModuleAndPackageDocFragment(
                    classifier = Package,
                    name = "com.my.package",
                    documentation = "My Package"
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
                ModuleAndPackageDocFragment(
                    classifier = Module,
                    name = "MyModule",
                    documentation = "D1"
                ),
                ModuleAndPackageDocFragment(
                    classifier = Package,
                    name = "com.sample",
                    documentation = "D2"
                )
            ),
            parseModuleAndPackageDocFragments(file)
        )
    }


    private fun source(documentation: String): ModuleAndPackageDocumentationSource =
        object : ModuleAndPackageDocumentationSource() {
            override val sourceDescription: String = "inline test"
            override val documentation: String = documentation
        }
}
