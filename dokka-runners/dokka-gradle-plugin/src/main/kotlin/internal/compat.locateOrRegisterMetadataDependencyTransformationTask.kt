/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package org.jetbrains.dokka.gradle.internal

import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyTransformationTask
import org.jetbrains.kotlin.gradle.plugin.mpp.locateOrRegisterMetadataDependencyTransformationTask

/**
 * Workaround for KT-80551.
 *
 * This function is defined in a separate file because it is `internal`,
 * and suppressing `INVISIBLE_MEMBER`/`INVISIBLE_REFERENCE`
 * on extension functions must be applied for the entire file.
 * Defining this function in a separate file reduces the risk of accidentally using other internal code.
 *
 * @see org.jetbrains.dokka.gradle.adapters.TransformedMetadataDependencyProvider
 */
internal fun Project.locateOrRegisterMetadataDependencyTransformationTaskCompat(
    sourceSet: KotlinSourceSet
): TaskProvider<MetadataDependencyTransformationTask>? =
    locateOrRegisterMetadataDependencyTransformationTask(sourceSet)
