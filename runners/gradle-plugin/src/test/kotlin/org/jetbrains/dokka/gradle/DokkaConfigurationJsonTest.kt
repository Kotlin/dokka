package org.jetbrains.dokka.gradle

import org.gradle.kotlin.dsl.withType
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.dokka.*
import java.io.File
import java.net.URL
import kotlin.test.Test
import kotlin.test.assertEquals

class DokkaConfigurationJsonTest {

    @Test
    fun `DokkaTask configuration toJsonString then parseJson`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("org.jetbrains.dokka")
        val dokkaTask = project.tasks.withType<DokkaTask>().first()
        dokkaTask.plugins.withDependencies { dependencies ->
            dependencies.clear()
        }
        dokkaTask.apply {
            this.failOnWarning = true
            this.offlineMode = true
            this.outputDirectory = File("customOutputDir")
            this.cacheRoot = File("customCacheRoot")
            this.pluginsConfiguration["0"] = "a"
            this.pluginsConfiguration["1"] = "b"
            this.dokkaSourceSets.create("main") { sourceSet ->
                sourceSet.moduleDisplayName = "moduleDisplayName"
                sourceSet.displayName = "customSourceSetDisplayName"
                sourceSet.reportUndocumented = true

                sourceSet.externalDocumentationLink { link ->
                    link.packageListUrl = URL("http://some.url")
                    link.url = URL("http://some.other.url")
                }
                sourceSet.perPackageOption { packageOption ->
                    packageOption.includeNonPublic = true
                    packageOption.reportUndocumented = true
                    packageOption.skipDeprecated = true
                }
            }
        }

        val sourceConfiguration = dokkaTask.buildDokkaConfiguration()
        val configurationJson = sourceConfiguration.toJsonString()
        val parsedConfiguration = DokkaConfigurationImpl(configurationJson)

        assertEquals(sourceConfiguration, parsedConfiguration)
        println(parsedConfiguration)
    }
}
