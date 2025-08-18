/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
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
    abstract class ClassLoader : WorkerIsolation() {
        // no options yet...
        override fun toString(): String = "WorkerIsolation.ClassLoader"
    }

    /**
     * Create a Worker using [process isolation][WorkerExecutor.processIsolation].
     *
     * Gradle will launch
     * [new Worker Daemon](https://docs.gradle.org/8.10/userguide/worker_api.html#worker-daemons),
     * re-using it across builds.
     *
     * @see org.gradle.workers.ProcessWorkerSpec
     * @see WorkerExecutor.processIsolation
     */
    abstract class Process : WorkerIsolation() {
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

        override fun toString(): String = "WorkerIsolation.Process"
    }

//    object None : WorkerIsolation() {
//        override fun toString(): String = "WorkerIsolation.None"
//        override fun equals(other: Any?): Boolean {
//            return other is None
//        }
//
//        override fun hashCode(): Int = toString().hashCode()
//    }
}

/** @see WorkerIsolation.ClassLoader */
typealias ClassLoaderIsolation = WorkerIsolation.ClassLoader


//typealias NoIsolation = WorkerIsolation.None

/** @see WorkerIsolation.Process */
typealias ProcessIsolation = WorkerIsolation.Process
