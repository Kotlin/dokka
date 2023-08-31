/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.kotlin

import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet


internal fun KotlinSourceSet.withAllDependentSourceSets(): Sequence<KotlinSourceSet> {
    return sequence {
        yield(this@withAllDependentSourceSets)
        for (dependentSourceSet in dependsOn) {
            yieldAll(dependentSourceSet.withAllDependentSourceSets())
        }
    }
}
