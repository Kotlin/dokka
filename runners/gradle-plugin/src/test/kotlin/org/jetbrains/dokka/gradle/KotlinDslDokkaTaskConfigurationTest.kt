package org.jetbrains.dokka.gradle

import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import kotlin.test.Test
import kotlin.test.assertEquals

class KotlinDslDokkaTaskConfigurationTest {

    @Test
    fun `configure project using dokka extension function`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("org.jetbrains.dokka")
        project.dokka { this.outputFormat = "test" }

        project.tasks.withType(DokkaTask::class.java).forEach { dokkaTask ->
            assertEquals("test", dokkaTask.outputFormat)
        }
    }

    @Test
    fun `sourceSet dependsOn by String`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("org.jetbrains.dokka")

        project.dokka {
            dokkaSourceSets.run {
                val commonMain = create("commonMain")
                val jvmMain = create("jvmMain") {
                    it.dependsOn("commonMain")
                }

                assertEquals(
                    0, commonMain.dependentSourceSets.size,
                    "Expected no dependent source set in commonMain"
                )

                assertEquals(
                    1, jvmMain.dependentSourceSets.size,
                    "Expected only one dependent source set in jvmMain"
                )

                assertEquals(
                    commonMain.sourceSetID, jvmMain.dependentSourceSets.single(),
                    "Expected jvmMain to depend on commonMain"
                )

                assertEquals(
                    DokkaSourceSetID(project.path, "commonMain"), commonMain.sourceSetID
                )
            }
        }
    }

    @Test
    fun `sourceSet dependsOn by DokkaSourceSet`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("org.jetbrains.dokka")

        project.dokka {
            dokkaSourceSets.run {
                val commonMain = create("commonMain")
                val jvmMain = create("jvmMain") {
                    it.dependsOn(commonMain)
                }

                assertEquals(
                    commonMain.sourceSetID, jvmMain.dependentSourceSets.single()
                )
            }
        }
    }

    @Test
    fun `sourceSet dependsOn by KotlinSourceSet`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("org.jetbrains.dokka")
        project.plugins.apply("org.jetbrains.kotlin.jvm")

        val kotlin = project.extensions.getByName("kotlin") as KotlinJvmProjectExtension

        project.dokka {
            dokkaSourceSets.run {
                val special = create("special") {
                    it.dependsOn(kotlin.sourceSets.getByName("main"))
                }

                assertEquals(
                    DokkaSourceSetID(project, "main"), special.dependentSourceSets.single()
                )
            }
        }
    }
}
