/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle

import org.gradle.kotlin.dsl.withType
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.PluginConfigurationImpl
import org.jetbrains.dokka.gradle.utils.create_
import org.jetbrains.dokka.gradle.utils.externalDocumentationLink_
import org.jetbrains.dokka.gradle.utils.withDependencies_
import org.jetbrains.dokka.testApi.assertDokkaConfigurationEquals
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals

class DokkaConfigurationSerializableTest {

    @Test
    fun `DokkaTask configuration write to file then parse`(@TempDir tempDirectory: File) {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("org.jetbrains.dokka")
        val dokkaTask = project.tasks.withType<DokkaTask>().first()
        dokkaTask.plugins.withDependencies_ { clear() }
        dokkaTask.apply {
            this.failOnWarning.set(true)
            this.offlineMode.set(true)
            this.outputDirectory.set(File("customOutputDir"))
            this.cacheRoot.set(File("customCacheRoot"))
            this.pluginsConfiguration.add(
                PluginConfigurationImpl(
                    "A",
                    DokkaConfiguration.SerializationFormat.JSON,
                    """ { "key" : "value1" } """
                )
            )
            this.pluginsConfiguration.add(
                PluginConfigurationImpl(
                    "B",
                    DokkaConfiguration.SerializationFormat.JSON,
                    """ { "key" : "value2" } """
                )
            )
            this.dokkaSourceSets.create_("main") {
                displayName.set("customSourceSetDisplayName")
                reportUndocumented.set(true)

                externalDocumentationLink_ {
                    packageListUrl.set(URI("http://some.url").toURL())
                    url.set(URI("http://some.other.url").toURL())
                }

                perPackageOption {
                    includeNonPublic.set(true)
                    reportUndocumented.set(true)
                    skipDeprecated.set(true)
                    documentedVisibilities.set(setOf(DokkaConfiguration.Visibility.PRIVATE))
                }
            }
        }

        val sourceConfiguration = dokkaTask.buildDokkaConfiguration()
        val configurationFile = tempDirectory.resolve("config.bin")
        ObjectOutputStream(configurationFile.outputStream()).use { stream ->
            stream.writeObject(sourceConfiguration)
        }
        val parsedConfiguration = ObjectInputStream(configurationFile.inputStream()).use { stream ->
            stream.readObject() as DokkaConfiguration
        }

        assertDokkaConfigurationEquals(sourceConfiguration, parsedConfiguration)
    }
}
