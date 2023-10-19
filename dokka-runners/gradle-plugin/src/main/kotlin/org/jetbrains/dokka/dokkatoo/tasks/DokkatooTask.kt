/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.dokkatoo.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.CacheableTask
import org.jetbrains.dokka.dokkatoo.DokkatooBasePlugin
import org.jetbrains.dokka.dokkatoo.internal.DokkatooInternalApi
import javax.inject.Inject

/** Base Dokkatoo task */
@CacheableTask
abstract class DokkatooTask
@DokkatooInternalApi
constructor() : DefaultTask() {

    @get:Inject
    abstract val objects: ObjectFactory

    init {
        group = DokkatooBasePlugin.TASK_GROUP
    }
}
