/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package translators

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DInterface
import org.jetbrains.dokka.model.doc.Description
import org.jetbrains.dokka.model.doc.DocumentationLink
import org.jetbrains.dokka.model.dfs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Reproduces https://github.com/Kotlin/dokka/issues/4618
 *
 * Since Dokka 2.3.0, Java sources fail to resolve references to classes compiled from Kotlin
 * and coming from the classpath (other modules of the same build or libraries like the stdlib):
 * supertypes are dropped and Javadoc links are rendered as plain text, with
 * `Couldn't resolve JavaDoc link` warnings.
 *
 * The test uses the Kotlin stdlib as the Kotlin-compiled classpath dependency:
 * the analyzed sources are pure Java, and everything they reference is only on the classpath.
 */
class JavaSourceKotlinClasspathResolutionTest : BaseAbstractTest() {

    val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/main/java")
                classpath = listOfNotNull(jvmStdlibPath)
            }
        }
    }

    @Test
    fun `java class should resolve kotlin supertype from classpath`() {
        testInline(
            """
            |/src/main/java/b/FromKotlin.java
            |package b;
            |
            |public interface FromKotlin extends kotlin.sequences.Sequence<CharSequence> { }
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val fromKotlin = module.findClasslike("b", "FromKotlin") as DInterface

                val supertypeDris = fromKotlin.supertypes.values.flatten().map { it.typeConstructor.dri }
                assertTrue(
                    "Sequence" in supertypeDris.map { it.classNames },
                    "Expected kotlin.sequences.Sequence to be resolved as a supertype, but got: $supertypeDris"
                )
                assertEquals(
                    0,
                    logger.warningsCount,
                    "Supertype from a Kotlin classpath dependency must be resolved without warnings"
                )
            }
        }
    }

    @Test
    fun `javadoc link to kotlin classpath class should resolve`() {
        testInline(
            """
            |/src/main/java/b/WithLink.java
            |package b;
            |
            |public interface WithLink {
            |    /** Does nothing, see {@link kotlin.Unit}. */
            |    public void doNothing();
            |}
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val fn = module.dfs { it.name == "doNothing" && it is DFunction } as DFunction
                val description = fn.documentation.values.single()
                    .children.filterIsInstance<Description>().single()

                val link = description.dfs { it is DocumentationLink } as DocumentationLink
                assertEquals("kotlin", link.dri.packageName)
                assertEquals("Unit", link.dri.classNames)
                assertEquals(0, logger.warningsCount, "There should be no unresolved JavaDoc link warnings")
            }
        }
    }
}
