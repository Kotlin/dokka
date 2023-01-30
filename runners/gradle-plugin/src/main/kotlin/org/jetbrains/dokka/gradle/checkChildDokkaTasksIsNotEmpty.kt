package org.jetbrains.dokka.gradle

import org.jetbrains.dokka.DokkaException

internal fun AbstractDokkaParentTask.checkChildDokkaTasksIsNotEmpty() {
    if (childDokkaTasks.isEmpty()) {
        throw DokkaException(
            """
            The ${this::class.java.simpleName} $path could not find any registered child task. 
            
            Please make sure to apply the dokka plugin to all included (sub)-projects individually e.g.:
            
            // subproject build.gradle.kts
            plugins {
                id("org.jetbrains.dokka")
            }
             
            or 
            
            // parent build.gradle.kts
            subprojects {
                plugins.apply("org.jetbrains.dokka")
            }
            """.trimIndent()
        )
    }
}
