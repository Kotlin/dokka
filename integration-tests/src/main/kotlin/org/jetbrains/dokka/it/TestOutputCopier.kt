package org.jetbrains.dokka.it

import org.junit.After
import java.io.File

interface TestOutputCopier {
    val projectOutputLocation: File

    @After
    fun copyToLocation() {
        System.getenv("DOKKA_TEST_OUTPUT_PATH")?.also { location ->
            println("Copying to ${File(location).absolutePath}")
            projectOutputLocation.copyRecursively(File(location))
        } ?: println("No copy path via env. varbiable 'DOKKA_PATH_TEST_OUTPUT' provided, skipping")
    }
}
