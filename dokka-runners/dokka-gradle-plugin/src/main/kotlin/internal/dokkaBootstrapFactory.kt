/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.internal

import org.jetbrains.dokka.DokkaBootstrap
import org.jetbrains.dokka.DokkaBootstrapImpl
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.toCompactJsonString
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.net.URLClassLoader
import java.util.concurrent.atomic.AtomicReference
import java.util.function.BiConsumer
import kotlin.reflect.KClass

internal class DokkaBootstrapProxy
// this constructor is used in tests (DokkaBootstrapTest)
internal constructor(
    runtimeClassLoader: ClassLoader,
    bootstrapClass: KClass<out DokkaBootstrap>
) : DokkaBootstrap {
    constructor(
        classpath: Set<File>,
        bootstrapClass: KClass<out DokkaBootstrap>
    ) : this(
        runtimeClassLoader = URLClassLoader(
            classpath.map { it.toURI().toURL() }.toTypedArray(),
            ClassLoader.getSystemClassLoader().parent
        ),
        bootstrapClass = bootstrapClass
    )

    private val runtimeClassloaderBootstrapClass = runtimeClassLoader.loadClass(bootstrapClass.qualifiedName)
    private val runtimeClassloaderBootstrapInstance =
        runtimeClassloaderBootstrapClass.constructors.first().newInstance()

    private fun invokeMethod(
        name: String,
        parameterTypes: Array<Class<*>>,
        args: Array<Any?>
    ): Any? = try {
        runtimeClassloaderBootstrapClass
            .getMethod(name, *parameterTypes)
            .invoke(runtimeClassloaderBootstrapInstance, *args)
    } catch (e: InvocationTargetException) {
        throw e.targetException
    }

    override fun configure(serializedConfigurationJSON: String, logger: BiConsumer<String, String>) {
        invokeMethod(
            name = "configure",
            parameterTypes = arrayOf(String::class.java, BiConsumer::class.java),
            args = arrayOf(serializedConfigurationJSON, logger)
        )
    }

    override fun generate() {
        invokeMethod(
            name = "generate",
            parameterTypes = emptyArray(),
            args = emptyArray()
        )
    }
}

internal fun generateDocumentationViaDokkaBootstrap(
    dokkaClasspath: Set<File>,
    dokkaConfiguration: DokkaConfiguration,
    logger: BiConsumer<String, String>
) {
    DokkaBootstrapProxy(dokkaClasspath, DokkaBootstrapImpl::class).apply {
        configure(dokkaConfiguration.toCompactJsonString(), logger)
        val uncaughtExceptionHolder = AtomicReference<Throwable?>()
        /**
         * Run in a new thread to avoid memory leaks that are related to ThreadLocal (that keeps `URLCLassLoader`)
         * Without a new thread all `ThreadLocal in the compiler/IDE codebase will leak.
         */
        Thread { generate() }.apply {
            setUncaughtExceptionHandler { _, throwable -> uncaughtExceptionHolder.set(throwable) }
            start()
            join()
        }
        uncaughtExceptionHolder.get()?.let { throw it }
    }
}


