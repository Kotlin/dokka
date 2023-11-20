/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle

import org.gradle.api.artifacts.Configuration
import org.jetbrains.dokka.DokkaBootstrap
import java.net.URLClassLoader
import kotlin.reflect.KClass

fun DokkaBootstrap(configuration: Configuration, bootstrapClass: KClass<out DokkaBootstrap>): DokkaBootstrap {
    val runtimeJars = configuration.resolve()
    val runtimeClassLoader = URLClassLoader(
        runtimeJars.map { it.toURI().toURL() }.toTypedArray(),
        ClassLoader.getSystemClassLoader().parent
    )

    val runtimeClassloaderBootstrapClass = runtimeClassLoader.loadClass(bootstrapClass.qualifiedName)
    val runtimeClassloaderBootstrapInstance = runtimeClassloaderBootstrapClass.constructors.first().newInstance()
    return automagicTypedProxy(DokkaPlugin::class.java.classLoader, runtimeClassloaderBootstrapInstance)
}
