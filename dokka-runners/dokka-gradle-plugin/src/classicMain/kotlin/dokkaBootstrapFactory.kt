/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle

import org.gradle.api.artifacts.Configuration
import org.jetbrains.dokka.gradle.internal.DokkaBootstrap
import org.jetbrains.dokka.DokkaBootstrap
import kotlin.reflect.KClass


@Deprecated(DOKKA_V1_DEPRECATION_MESSAGE)
@Suppress("DeprecatedCallableAddReplaceWith")
fun DokkaBootstrap(configuration: Configuration, bootstrapClass: KClass<out DokkaBootstrap>): DokkaBootstrap {
    return DokkaBootstrap(
        classpath = configuration.resolve(),
        bootstrapClass = bootstrapClass,
    )
}
