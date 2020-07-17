package org.jetbrains.dokka.gradle

import org.gradle.api.artifacts.Configuration
import org.jetbrains.dokka.DokkaBootstrap
import java.net.URLClassLoader


fun DokkaBootstrap(configuration: Configuration, bootstrapClassFQName: String): DokkaBootstrap {
    val runtimeJars = configuration.resolve()
    val runtimeClassLoader = URLClassLoader(
        runtimeJars.map { it.toURI().toURL() }.toTypedArray(),
        ClassLoader.getSystemClassLoader().parent
    )

    val bootstrapClass = runtimeClassLoader.loadClass(bootstrapClassFQName)
    val bootstrapInstance = bootstrapClass.constructors.first().newInstance()
    return automagicTypedProxy(DokkaPlugin::class.java.classLoader, bootstrapInstance)
}
