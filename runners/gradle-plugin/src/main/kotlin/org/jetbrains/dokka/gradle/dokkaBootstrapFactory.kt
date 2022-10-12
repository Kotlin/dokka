@file:Suppress("FunctionName")

package org.jetbrains.dokka.gradle

import org.gradle.api.file.FileCollection
import org.jetbrains.dokka.DokkaBootstrap
import java.net.URLClassLoader
import kotlin.reflect.KClass

fun DokkaBootstrap(runtimeJars: FileCollection, bootstrapClass: KClass<out DokkaBootstrap>): DokkaBootstrap {
    val runtimeClassLoader = URLClassLoader(
        runtimeJars.map { it.toURI().toURL() }.toTypedArray(),
        ClassLoader.getSystemClassLoader().parent
    )

    val runtimeClassloaderBootstrapClass = runtimeClassLoader.loadClass(bootstrapClass.qualifiedName)
    val runtimeClassloaderBootstrapInstance = runtimeClassloaderBootstrapClass.constructors.first().newInstance()
    return automagicTypedProxy(DokkaPlugin::class.java.classLoader, runtimeClassloaderBootstrapInstance)
}
