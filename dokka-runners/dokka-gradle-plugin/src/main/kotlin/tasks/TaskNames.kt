/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.tasks

import org.jetbrains.dokka.gradle.internal.DokkaInternalApi
import org.jetbrains.dokka.gradle.internal.HasFormatName

@DokkaInternalApi
class TaskNames(override val formatName: String) : HasFormatName() {
    val generate = "dokkaGenerate".appendFormat()
    val generatePublication = "dokkaGeneratePublication".appendFormat()
    val generateModule = "dokkaGenerateModule".appendFormat()
}
