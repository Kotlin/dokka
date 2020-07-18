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
