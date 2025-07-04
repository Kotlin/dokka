/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle

import org.gradle.kotlin.dsl.withType
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.PluginConfigurationImpl
import org.jetbrains.dokka.gradle.utils.create_
import org.jetbrains.dokka.gradle.utils.enableV1Plugin
import org.jetbrains.dokka.gradle.utils.externalDocumentationLink_
import org.jetbrains.dokka.gradle.utils.withDependencies_
import org.jetbrains.dokka.testApi.assertDokkaConfigurationEquals
import org.jetbrains.dokka.toCompactJsonString
import java.io.File
import java.net.URI
import kotlin.test.Test

class DokkaConfigurationJsonTest {

    @Test
    fun `DokkaTask configuration toJsonString then parseJson`() {
        val project = ProjectBuilder.builder().build()
            .enableV1Plugin()
        project.plugins.apply("org.jetbrains.dokka")
        val dokkaTask = project.tasks.withType<@Suppress("DEPRECATION")DokkaTask>().first()
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
        val configurationJson = sourceConfiguration.toCompactJsonString()
        val parsedConfiguration = DokkaConfigurationImpl(configurationJson)

        assertDokkaConfigurationEquals(sourceConfiguration, parsedConfiguration)
        println(parsedConfiguration)
    }
}
