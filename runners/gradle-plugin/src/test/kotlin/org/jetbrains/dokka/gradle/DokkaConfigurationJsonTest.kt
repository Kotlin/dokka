@file:Suppress("UnstableApiUsage")

package org.jetbrains.dokka.gradle

import org.gradle.kotlin.dsl.withType
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.dokka.*
import org.jetbrains.dokka.DokkaDefaults.includeNonPublic
import org.jetbrains.dokka.DokkaDefaults.reportUndocumented
import java.io.File
import java.net.URL
import kotlin.test.Test
import kotlin.test.assertEquals
import org.jetbrains.dokka.gradle.tasks.*

class DokkaConfigurationJsonTest {

    @Test
    fun `DokkaTask configuration toJsonString then parseJson`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("org.jetbrains.dokka")
        val dokkaTask = project.tasks.withType<DokkaTask>().first()
        dokkaTask.plugins.withDependencies { clear() }
        dokkaTask.apply {
            this.failOnWarning by true
            this.offlineMode by true
            this.outputDirectory by File("customOutputDir")
            this.cacheRoot by File("customCacheRoot")
            this.pluginsConfiguration.add(PluginConfigurationImpl("A", DokkaConfiguration.SerializationFormat.JSON, """ { "key" : "value1" } """))
            this.pluginsConfiguration.add(PluginConfigurationImpl("B", DokkaConfiguration.SerializationFormat.JSON, """ { "key" : "value2" } """))
            this.dokkaSourceSets.create("main") {
                displayName by "customSourceSetDisplayName"
                reportUndocumented by true

                externalDocumentationLink {
                    packageListUrl by URL("http://some.url")
                    url by URL("http://some.other.url")
                }
                perPackageOption {
                    includeNonPublic by true
                    reportUndocumented by true
                    skipDeprecated by true
                    documentedVisibilities by setOf(DokkaConfiguration.Visibility.PRIVATE)
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
