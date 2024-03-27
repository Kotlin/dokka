/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package dokkabuild.internal

import org.gradle.api.artifacts.Configuration

/**
 * Mark this [Configuration] as one that will be consumed by other subprojects.
 *
 * ```
 * isCanBeResolved = false
 * isCanBeConsumed = true
 * isCanBeDeclared = false
 * ```
 */
fun Configuration.consumable(
    visible: Boolean = false
) {
    isVisible = visible
    isCanBeResolved = false
    isCanBeConsumed = true
    isCanBeDeclared = false
}

/**
 * Mark this [Configuration] as one that will consume artifacts from other subprojects (also known as 'resolving')
 *
 * ```
 * isCanBeResolved = true
 * isCanBeConsumed = false
 * isCanBeDeclared = false
 * ```
 */
fun Configuration.resolvable(
    visible: Boolean = false
) {
    isVisible = visible
    isCanBeResolved = true
    isCanBeConsumed = false
    isCanBeDeclared = false
}

/**
 * Mark this [Configuration] as one that will be used to declare dependencies.
 *
 * ```
 * isCanBeResolved = false
 * isCanBeConsumed = false
 * isCanBeDeclared = true
 * ```
 */
fun Configuration.declarable(
    visible: Boolean = false
) {
    isVisible = visible
    isCanBeResolved = false
    isCanBeConsumed = false
    isCanBeDeclared = true
}
