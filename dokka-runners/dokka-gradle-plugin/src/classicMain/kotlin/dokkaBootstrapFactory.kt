/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle

import org.gradle.api.artifacts.Configuration
import org.jetbrains.dokka.DokkaBootstrap
import org.jetbrains.dokka.gradle.internal.DokkaBootstrapProxy
import kotlin.reflect.KClass


@Deprecated(DOKKA_V1_DEPRECATION_MESSAGE)
@Suppress("DeprecatedCallableAddReplaceWith")
fun DokkaBootstrap(configuration: Configuration, bootstrapClass: KClass<out DokkaBootstrap>): DokkaBootstrap {
    return DokkaBootstrapProxy(
        classpath = configuration.resolve(),
        bootstrapClass = bootstrapClass,
    )
}
