/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.CacheableTask
import org.jetbrains.dokka.gradle.DokkaBasePlugin
import org.jetbrains.dokka.gradle.internal.DokkaInternalApi
import javax.inject.Inject

/** Base Dokka task */
@CacheableTask
abstract class DokkaBaseTask
@DokkaInternalApi
constructor() : DefaultTask() {

    @get:Inject
    abstract val objects: ObjectFactory

    init {
        group = DokkaBasePlugin.TASK_GROUP
    }
}
