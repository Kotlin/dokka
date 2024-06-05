/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.it.cli

import org.jetbrains.dokka.it.AbstractIntegrationTest
import org.jetbrains.dokka.it.systemProperty
import java.io.File

public abstract class AbstractCliIntegrationTest : AbstractIntegrationTest() {
    /** The Dokka CLI JAR. */
    protected val dokkaCliJarPath: String by systemProperty {
        it.split(File.pathSeparatorChar)
            .singleOrNull()
            ?: error("Expected a single Dokka CLI JAR, but got $it")
    }

    /** Classpath required for running the Dokka CLI, delimited by `;`. */
    protected val dokkaPluginsClasspath: String by systemProperty {
        it.replace(File.pathSeparatorChar, ';')
    }
}
