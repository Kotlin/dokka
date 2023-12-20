/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package transformers

import org.jetbrains.dokka.DokkaDefaults
import org.jetbrains.dokka.PackageOptionsImpl
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.links.DRI
import kotlin.test.Test
import kotlin.test.assertEquals

class SuppressedByConfigurationDocumentableFilterTransformerTest : BaseAbstractTest() {

    @Test
    fun `class filtered by package options`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src")
                    perPackageOptions = listOf(
                        packageOptions(matchingRegex = "suppressed.*", suppress = true),
                        packageOptions(matchingRegex = "default.*", suppress = false)
                    )
                }
            }
        }

        testInline(
            """
            /src/suppressed/Suppressed.kt
            package suppressed
            class Suppressed
            
            /src/default/Default.kt
            package default
            class Default.kt
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                assertEquals(1, module.children.size, "Expected just a single package in module")
                assertEquals(1, module.packages.size, "Expected just a single package in module")

                val pkg = module.packages.single()
                assertEquals("default", pkg.dri.packageName, "Expected 'default' package in module")
                assertEquals(1, pkg.children.size, "Expected just a single child in 'default' package")
                assertEquals(1, pkg.classlikes.size, "Expected just a single child in 'default' package")

                val classlike = pkg.classlikes.single()
                assertEquals(DRI("default", "Default"), classlike.dri, "Expected 'Default' class in 'default' package")
            }
        }
    }

    @Test
    fun `class filtered by more specific package options`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src")
                    perPackageOptions = listOf(
                        packageOptions(matchingRegex = "parent.some.*", suppress = false),
                        packageOptions(matchingRegex = "parent.some.suppressed.*", suppress = true),

                        packageOptions(matchingRegex = "parent.other.*", suppress = true),
                        packageOptions(matchingRegex = "parent.other.default.*", suppress = false)
                    )
                }
            }
        }

        testInline(
            """
            /src/parent/some/Some.kt
            package parent.some
            class Some
            
            /src/parent/some/suppressed/Suppressed.kt
            package parent.some.suppressed
            class Suppressed
            
            /src/parent/other/Other.kt
            package parent.other
            class Other
            
            /src/parent/other/default/Default.kt
            package parent.other.default
            class Default
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                assertEquals(2, module.packages.size, "Expected two packages in module")
                assertEquals(
                    listOf(DRI("parent.some"), DRI("parent.other.default")).sortedBy { it.packageName },
                    module.packages.map { it.dri }.sortedBy { it.packageName },
                    "Expected 'parent.some' and 'parent.other.default' packages to be not suppressed"
                )
            }
        }
    }

    @Test
    fun `class filtered by parent file path`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src")
                    suppressedFiles = listOf("src/suppressed")
                }
            }
        }

        testInline(
            """
            /src/suppressed/Suppressed.kt
            package suppressed
            class Suppressed
            
            /src/default/Default.kt
            package default
            class Default.kt
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                assertEquals(1, module.children.size, "Expected just a single package in module")
                assertEquals(1, module.packages.size, "Expected just a single package in module")

                val pkg = module.packages.single()
                assertEquals("default", pkg.dri.packageName, "Expected 'default' package in module")
                assertEquals(1, pkg.children.size, "Expected just a single child in 'default' package")
                assertEquals(1, pkg.classlikes.size, "Expected just a single child in 'default' package")

                val classlike = pkg.classlikes.single()
                assertEquals(DRI("default", "Default"), classlike.dri, "Expected 'Default' class in 'default' package")
            }
        }
    }

    @Test
    fun `class filtered by exact file path`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src")
                    suppressedFiles = listOf("src/suppressed/Suppressed.kt")
                }
            }
        }

        testInline(
            """
            /src/suppressed/Suppressed.kt
            package suppressed
            class Suppressed
            
            /src/default/Default.kt
            package default
            class Default.kt
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                assertEquals(1, module.children.size, "Expected just a single package in module")
                assertEquals(1, module.packages.size, "Expected just a single package in module")

                val pkg = module.packages.single()
                assertEquals("default", pkg.dri.packageName, "Expected 'default' package in module")
                assertEquals(1, pkg.children.size, "Expected just a single child in 'default' package")
                assertEquals(1, pkg.classlikes.size, "Expected just a single child in 'default' package")

                val classlike = pkg.classlikes.single()
                assertEquals(DRI("default", "Default"), classlike.dri, "Expected 'Default' class in 'default' package")
            }
        }
    }

    private fun packageOptions(
        matchingRegex: String,
        suppress: Boolean
    ) = PackageOptionsImpl(
        matchingRegex = matchingRegex,
        suppress = suppress,
        includeNonPublic = true,
        documentedVisibilities = DokkaDefaults.documentedVisibilities,
        reportUndocumented = false,
        skipDeprecated = false
    )

}
