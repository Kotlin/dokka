package org.jetbrains.dokka.gradle.workers

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.process.JavaForkOptions
import org.gradle.workers.WorkerExecutor


/**
 * Configure how a Gradle Worker is created using [org.gradle.workers.WorkerExecutor].
 *
 * @see WorkerExecutor.classLoaderIsolation
 * @see WorkerExecutor.processIsolation
 */
sealed class WorkerIsolation {

    /**
     * Execute a Worker in the current Gradle process, with an
     * [isolated classpath][WorkerExecutor.classLoaderIsolation].
     *
     * Presently there are no options to configure the behaviour of a classloader-isolated worker.
     *
     * @see org.gradle.workers.ClassLoaderWorkerSpec
     * @see WorkerExecutor.classLoaderIsolation
     */
    abstract class ClassLoaderIsolation : WorkerIsolation() {
        // no options yet...
    }

    /**
     * Create a Worker using [process isolation][WorkerExecutor.processIsolation].
     *
     * Gradle will launch
     * [new Worker Daemon](https://docs.gradle.org/8.5/userguide/worker_api.html#creating_a_worker_daemon)
     * re-using it across builds.
     *
     * @see org.gradle.workers.ProcessWorkerSpec
     * @see WorkerExecutor.processIsolation
     */
    abstract class ProcessIsolation : WorkerIsolation() {
        /** @see JavaForkOptions.setDebug */
        @get:Input
        @get:Optional
        abstract val debug: Property<Boolean>

        /** @see JavaForkOptions.setEnableAssertions */
        @get:Input
        @get:Optional
        abstract val enableAssertions: Property<Boolean>

        /** @see JavaForkOptions.setMinHeapSize */
        @get:Input
        @get:Optional
        abstract val minHeapSize: Property<String>

        /** @see JavaForkOptions.setMaxHeapSize */
        @get:Input
        @get:Optional
        abstract val maxHeapSize: Property<String>

        /** @see JavaForkOptions.setJvmArgs */
        @get:Input
        @get:Optional
        abstract val jvmArgs: ListProperty<String>

        /** @see JavaForkOptions.setDefaultCharacterEncoding */
        @get:Input
        @get:Optional
        abstract val defaultCharacterEncoding: Property<String>

        /** @see JavaForkOptions.setSystemProperties */
        @get:Input
        @get:Optional
        abstract val systemProperties: MapProperty<String, Any>
    }
}

/** @see WorkerIsolation.ClassLoaderIsolation */
typealias ClassLoaderIsolation = WorkerIsolation.ClassLoaderIsolation

/** @see WorkerIsolation.ProcessIsolation */
typealias ProcessIsolation = WorkerIsolation.ProcessIsolation
