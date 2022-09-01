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
        } ?: println("No path via env. varbiable 'DOKKA_TEST_OUTPUT_PATH' provided, skipping copying")
    }
}
