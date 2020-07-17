package org.jetbrains.dokka.it.cli

import org.jetbrains.dokka.it.AbstractIntegrationTest
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.assertTrue

abstract class AbstractCliIntegrationTest : AbstractIntegrationTest() {

    protected val cliJarFile: File by lazy {
        File(temporaryTestFolder.root, "dokka.jar")
    }

    protected val basePluginJarFile: File by lazy {
        File(temporaryTestFolder.root, "base-plugin.jar")
    }

    @BeforeTest
    fun copyJarFiles() {
        val cliJarPathEnvironmentKey = "CLI_JAR_PATH"
        val cliJarFile = File(System.getenv(cliJarPathEnvironmentKey))
        assertTrue(
            cliJarFile.exists() && cliJarFile.isFile,
            "Missing path to CLI jar System.getenv($cliJarPathEnvironmentKey)"
        )
        cliJarFile.copyTo(this.cliJarFile)

        val basePluginPathEnvironmentKey = "BASE_PLUGIN_JAR_PATH"
        val basePluginJarFile = File(System.getenv(basePluginPathEnvironmentKey))
        assertTrue(
            basePluginJarFile.exists() && basePluginJarFile.isFile,
            "Missing path to base plugin jar System.getenv($basePluginPathEnvironmentKey)"
        )
        basePluginJarFile.copyTo(this.basePluginJarFile)
    }
}
