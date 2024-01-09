/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.tasks

import org.gradle.api.Task

// task name: dokkaGenerate
//  will run all other `DokkaGenerateTask`
public interface DokkaGenerateAllTask : Task

// dokkaGenerate{Classifier}
//  classifier can be:
//  - any format: html, javadoc, etc (dokkaGenerateHtml)
//  - when aggregation is enabled, will be MultiModule{format}/Collected{format} (dokkaGenerateMultiModuleHtml)
public interface DokkaGenerateTask : Task
