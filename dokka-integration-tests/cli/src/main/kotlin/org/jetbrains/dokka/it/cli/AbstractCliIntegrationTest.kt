/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it.cli

import org.jetbrains.dokka.it.AbstractIntegrationTest
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.assertTrue

public abstract class AbstractCliIntegrationTest : AbstractIntegrationTest() {

    protected val cliJarFile: File by lazy {
        File(tempFolder, "dokka.jar")
    }

    protected val basePluginJarFile: File by lazy {
        File(tempFolder, "base-plugin.jar")
    }

    @BeforeTest
    public fun copyJarFiles() {
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
