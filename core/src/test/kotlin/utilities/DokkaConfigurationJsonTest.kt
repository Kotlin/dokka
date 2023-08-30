/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package utilities

import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.dokka.DokkaSourceSetImpl
import org.jetbrains.dokka.toCompactJsonString
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class DokkaConfigurationJsonTest {
    @Test
    fun `simple configuration toJsonString then parseJson`() {
        val configuration = DokkaConfigurationImpl(
            moduleName = "moduleName",
            outputDir = File("customOutputDir"),
            pluginsClasspath = listOf(File("plugins/customPlugin.jar")),
            sourceSets = listOf(
                DokkaSourceSetImpl(
                    sourceRoots = setOf(File("customSourceRoot")),
                    sourceSetID = DokkaSourceSetID("customModuleName", "customSourceSetName")
                )
            )
        )

        val jsonString = configuration.toCompactJsonString()
        val parsedConfiguration = DokkaConfigurationImpl(jsonString)
        assertEquals(configuration, parsedConfiguration)
    }

    @Test
    fun `parse simple configuration json`() {
        val json = """
            {
              "moduleName": "moduleName",
              "outputDir": "customOutputDir",
              "pluginsClasspath": [ "plugins/customPlugin.jar" ],
              "sourceSets": [
                {
                  "sourceSetID": {
                    "scopeId": "customModuleName",
                    "sourceSetName": "customSourceSetName"
                  },
                  "sourceRoots": [ "customSourceRoot" ], 
                  "classpath": [ "classpath/custom1.jar", "classpath/custom2.jar" ]
                }
              ]
            }
        """.trimIndent()

        val parsedConfiguration = DokkaConfigurationImpl(json)
        assertEquals(
            DokkaConfigurationImpl(
                moduleName = "moduleName",
                outputDir = File("customOutputDir"),
                pluginsClasspath = listOf(File("plugins/customPlugin.jar")),
                sourceSets = listOf(
                    DokkaSourceSetImpl(
                        sourceRoots = setOf(File("customSourceRoot")),
                        sourceSetID = DokkaSourceSetID("customModuleName", "customSourceSetName"),
                        classpath = listOf(File("classpath/custom1.jar"), File("classpath/custom2.jar"))
                    )
                )
            ),
            parsedConfiguration
        )
    }
}
