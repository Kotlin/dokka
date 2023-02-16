package org.jetbrains.dokka.gradle.workers

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.jetbrains.dokka.*
import org.jetbrains.dokka.gradle.internal.LoggerAdapter
import java.io.File
import java.net.URLClassLoader
import java.util.concurrent.Semaphore
import java.util.function.BiConsumer

/**
 * Gradle Worker Daemon for running [DokkaGenerator].
 *
 * The worker requires [DokkaGenerator] is present on the classpath.
 */
abstract class DokkaGeneratorWorker : WorkAction<DokkaGeneratorWorker.Parameters> {

    private val logger = LoggerAdapter(DokkaGeneratorWorker::class)

    interface Parameters : WorkParameters {
        val dokkaConfiguration: Property<DokkaConfigurationImpl>
    }

    override fun execute() {
        println("Execute DokkaGeneratorWorker on ${ ProcessHandle.current().pid()}")
        //println("Current classloader")
       // (this.javaClass.classLoader as URLClassLoader).urLs.forEach { println(it) }

        val dokkaConfiguration = parameters.dokkaConfiguration.get()
        logger.progress("Run ${dokkaConfiguration.moduleName} ${if(dokkaConfiguration.delayTemplateSubstitution) "Partial" else "MultiModule"} task on ${ ProcessHandle.current().pid()}" )

        val generator = DokkaGenerator(dokkaConfiguration, logger)
        generator.generate()
    }
}

abstract class DokkaGeneratorWithCachedClasspathWorker : WorkAction<DokkaGeneratorWithCachedClasspathWorker.Parameters> {

    private val logger = LoggerAdapter(DokkaGeneratorWithCachedClasspathWorker::class)

    interface Parameters : WorkParameters {
        val dokkaConfiguration: Property<DokkaConfigurationImpl>
        val dokkaClasspath: SetProperty<File>
    }

    private fun createProxyLogger(): BiConsumer<String, String> = BiConsumer { level, message ->
        when (level) {
            "debug" -> logger.debug(message)
            "info" -> logger.info(message)
            "progress" -> logger.progress(message)
            "warn" -> logger.warn(message)
            "error" -> logger.error(message)
        }
    }

    override fun execute() {
        println("Execute DokkaGeneratorWIthCachedClasspathWorker on ${ ProcessHandle.current().pid()}")
        //println("Current classloader")
        //(this.javaClass.classLoader as URLClassLoader).urLs.forEach { println(it) }

        val dokkaConfiguration = parameters.dokkaConfiguration.get()
        val dokkaClasspath= parameters.dokkaClasspath.get().toSet()
        val classloader = getClassloader(dokkaClasspath)
        val dokkaBootstrapImplClass= classloader.loadClass(DokkaBootstrapImpl::class.qualifiedName)
        val dokkaBootstrapImplInstance = dokkaBootstrapImplClass.constructors.first().newInstance()
        val configureMethod = dokkaBootstrapImplClass.methods.first{ it.name == "configure" }
        configureMethod.invoke(dokkaBootstrapImplInstance, dokkaConfiguration.toJsonString(), createProxyLogger())

        val generateMethod = dokkaBootstrapImplClass.methods.first{ it.name == "generate" }
        generateMethod.invoke(dokkaBootstrapImplInstance)
    }
        companion object {
            private val cachedClassloader: MutableMap<Set<File>, URLClassLoader> = mutableMapOf()
            private val mutex = Semaphore(1)
            private fun<T> synchronized(mutex: Semaphore, fn: () -> T): T {
                mutex.acquire(1)
                val value = fn()
                mutex.release()
                return value
            }
            fun getClassloader(classpath: Set<File>): URLClassLoader = synchronized(mutex) {
                cachedClassloader.computeIfAbsent(classpath) {
                    println("### load a new classloader ###")
                    URLClassLoader(classpath.map { File(it.path).toURI().toURL() }.toTypedArray(), null)
                }
            }

        }
}