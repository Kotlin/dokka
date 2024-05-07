/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.it.cli

import org.jetbrains.dokka.it.AbstractIntegrationTest
import org.jetbrains.dokka.it.systemProperty

public abstract class AbstractCliIntegrationTest : AbstractIntegrationTest() {
    /** The Dokka CLI JAR. */
    protected val dokkaCliJarPath: String by systemProperty()

    /** Classpath required for running the Dokka CLI, delimited by `;`. */
    protected val dokkaPluginsClasspath: String by systemProperty()
}
