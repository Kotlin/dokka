/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.internal

import org.jetbrains.dokka.DokkaBootstrap
import org.jetbrains.dokka.DokkaBootstrapImpl
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.dokka.gradle.automagicTypedProxy
import org.jetbrains.dokka.toCompactJsonString
import java.io.File
import java.net.URLClassLoader
import java.util.concurrent.atomic.AtomicReference
import java.util.function.BiConsumer
import kotlin.reflect.KClass

internal fun DokkaBootstrap(classpath: Set<File>, bootstrapClass: KClass<out DokkaBootstrap>): DokkaBootstrap {
    val runtimeClassLoader = URLClassLoader(
        classpath.map { it.toURI().toURL() }.toTypedArray(),
        ClassLoader.getSystemClassLoader().parent
    )

    val runtimeClassloaderBootstrapClass = runtimeClassLoader.loadClass(bootstrapClass.qualifiedName)
    val runtimeClassloaderBootstrapInstance = runtimeClassloaderBootstrapClass.constructors.first().newInstance()
    return automagicTypedProxy(
        DokkaPlugin::class.java.classLoader,
        runtimeClassloaderBootstrapInstance
    )
}

internal fun generateDocumentationViaDokkaBootstrap(
    dokkaClasspath: Set<File>,
    dokkaConfiguration: DokkaConfiguration,
    logger: BiConsumer<String, String>
) {
    DokkaBootstrap(dokkaClasspath, DokkaBootstrapImpl::class).apply {
        configure(dokkaConfiguration.toCompactJsonString(), logger)
        val uncaughtExceptionHolder = AtomicReference<Throwable?>()
        /**
         * Run in a new thread to avoid memory leaks that are related to ThreadLocal (that keeps `URLCLassLoader`)
         * Currently, all `ThreadLocal`s leaking are in the compiler/IDE codebase.
         */
        Thread { generate() }.apply {
            setUncaughtExceptionHandler { _, throwable -> uncaughtExceptionHolder.set(throwable) }
            start()
            join()
        }
        uncaughtExceptionHolder.get()?.let { throw it }
    }
}


