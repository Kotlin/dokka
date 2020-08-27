package org.jetbrains.dokka.gradle

import org.gradle.kotlin.dsl.withType
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.toJsonString
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.URL
import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("UnstableApiUsage")
class DokkaConfigurationSerializableTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `DokkaTask configuration write to file then parse`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("org.jetbrains.dokka")
        val dokkaTask = project.tasks.withType<DokkaTask>().first()
        dokkaTask.plugins.withDependencies { dependencies ->
            dependencies.clear()
        }
        dokkaTask.apply {
            this.failOnWarning by true
            this.offlineMode by true
            this.outputDirectory by File("customOutputDir")
            this.cacheRoot by File("customCacheRoot")
            this.pluginsConfiguration.put("0", "a")
            this.pluginsConfiguration.put("1", "b")
            this.dokkaSourceSets.create("main") { sourceSet ->
                sourceSet.displayName by "customSourceSetDisplayName"
                sourceSet.reportUndocumented by true

                sourceSet.externalDocumentationLink { link ->
                    link.packageListUrl by URL("http://some.url")
                    link.url by URL("http://some.other.url")
                }

                sourceSet.perPackageOption { packageOption ->
                    packageOption.includeNonPublic by true
                    packageOption.reportUndocumented by true
                    packageOption.skipDeprecated by true
                }
            }
        }

        val sourceConfiguration = dokkaTask.buildDokkaConfiguration()
        val configurationFile = temporaryFolder.root.resolve("config.bin")
        ObjectOutputStream(configurationFile.outputStream()).use { stream ->
            stream.writeObject(sourceConfiguration)
        }
        val parsedConfiguration = ObjectInputStream(configurationFile.inputStream()).use { stream ->
            stream.readObject() as DokkaConfiguration
        }

        assertEquals(sourceConfiguration, parsedConfiguration)
    }
}
