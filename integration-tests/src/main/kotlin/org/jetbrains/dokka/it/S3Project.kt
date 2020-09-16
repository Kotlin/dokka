package org.jetbrains.dokka.it

import org.junit.After
import java.io.File

interface S3Project {
    val projectOutputLocation: File

    @After
    fun copyToLocation() {
        System.getenv("DOKKA_IT_AWS_PATH")?.also { location ->
            println("Copying to ${File(location).absolutePath}")
            projectOutputLocation.copyRecursively(File(location))
        } ?: println("No copy path provided, skipping")
    }
}
