/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.tasks

import org.jetbrains.dokka.gradle.internal.DokkatooInternalApi
import org.jetbrains.dokka.gradle.internal.HasFormatName

@DokkatooInternalApi
class TaskNames(override val formatName: String) : HasFormatName() {
    val generate = "dokkatooGenerate".appendFormat()
    val generatePublication = "dokkatooGeneratePublication".appendFormat()
    val generateModule = "dokkatooGenerateModule".appendFormat()
    val consolidateModuleElements = "dokkatooConsolidateModuleElements".appendFormat()
    val prepareModuleDescriptor = "prepareDokkatooModuleDescriptor".appendFormat()
}
