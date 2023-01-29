package org.jetbrains.dokka.gradle.tasks

import org.gradle.api.DefaultTask
import org.jetbrains.dokka.gradle.DokkaPlugin

/** Base Dokka task */
abstract class DokkaTask : DefaultTask() {

    init {
        group = DokkaPlugin.TASK_GROUP
    }

}
