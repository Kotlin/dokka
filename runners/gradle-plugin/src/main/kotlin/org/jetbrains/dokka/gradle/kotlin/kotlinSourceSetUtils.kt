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
