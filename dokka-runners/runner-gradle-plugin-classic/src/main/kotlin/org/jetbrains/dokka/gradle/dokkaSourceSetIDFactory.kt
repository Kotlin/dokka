/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle

import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Task
import org.jetbrains.dokka.DokkaSourceSetID

internal fun DokkaSourceSetID(task: Task, sourceSetName: String): DokkaSourceSetID {
    return DokkaSourceSetID(task.path, sourceSetName)
}

@Suppress("FunctionName")
internal fun Task.DokkaSourceSetIdFactory() = NamedDomainObjectFactory<DokkaSourceSetID> { name ->
    DokkaSourceSetID(this@DokkaSourceSetIdFactory, name)
}
