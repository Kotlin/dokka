/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it

import java.io.File
import kotlin.test.AfterTest

public interface TestOutputCopier {
    public val projectOutputLocation: File

    @AfterTest
    public fun copyToLocation() {
        System.getenv("DOKKA_TEST_OUTPUT_PATH")?.also { location ->
            println("Copying to ${File(location).absolutePath}")
            projectOutputLocation.copyRecursively(File(location))
        } ?: println("No path via env. variable 'DOKKA_TEST_OUTPUT_PATH' provided, skipping copying")
    }
}
