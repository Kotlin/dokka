@file:Suppress("FunctionName")

package org.jetbrains.dokka.gradle

import org.jetbrains.dokka.DokkaBootstrap
import java.io.File
import java.net.URLClassLoader
import kotlin.reflect.KClass

fun DokkaBootstrap(runtimeJars: Set<File>, bootstrapClass: KClass<out DokkaBootstrap>): DokkaBootstrap {
    val runtimeClassLoader = URLClassLoader(
        runtimeJars.map { it.toURI().toURL() }.toTypedArray(),
        ClassLoader.getSystemClassLoader().parent
    )

    val runtimeClassloaderBootstrapClass = runtimeClassLoader.loadClass(bootstrapClass.qualifiedName)
    val runtimeClassloaderBootstrapInstance = runtimeClassloaderBootstrapClass.constructors.first().newInstance()
    return automagicTypedProxy(DokkaPlugin::class.java.classLoader, runtimeClassloaderBootstrapInstance)
}
