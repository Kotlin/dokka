/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle

import org.gradle.api.artifacts.Configuration
import org.jetbrains.dokka.DokkaBootstrap
import java.io.File
import java.net.URLClassLoader
import kotlin.reflect.KClass

internal fun DokkaBootstrap(classpath: Set<File>, bootstrapClass: KClass<out DokkaBootstrap>): DokkaBootstrap {
    val runtimeClassLoader = URLClassLoader(
        classpath.map { it.toURI().toURL() }.toTypedArray(),
        ClassLoader.getSystemClassLoader().parent
    )

    val runtimeClassloaderBootstrapClass = runtimeClassLoader.loadClass(bootstrapClass.qualifiedName)
    val runtimeClassloaderBootstrapInstance = runtimeClassloaderBootstrapClass.constructors.first().newInstance()
    return automagicTypedProxy(@Suppress("DEPRECATION") DokkaClassicPlugin::class.java.classLoader, runtimeClassloaderBootstrapInstance)
}

@Deprecated(DOKKA_V1_DEPRECATION_MESSAGE)
@Suppress("DeprecatedCallableAddReplaceWith")
fun DokkaBootstrap(configuration: Configuration, bootstrapClass: KClass<out DokkaBootstrap>): DokkaBootstrap {
    return DokkaBootstrap(
        classpath = configuration.resolve(),
        bootstrapClass = bootstrapClass,
    )
}
