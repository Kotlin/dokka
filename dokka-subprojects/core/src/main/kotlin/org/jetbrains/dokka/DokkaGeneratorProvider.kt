/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka

import org.jetbrains.dokka.utilities.DokkaLogger
import java.util.*

/**
 * Provider for an instance of [DokkaGenerator].
 *
 * The implementation will be loaded using [ServiceLoader].
 */
@InternalDokkaApi
public interface DokkaGeneratorProvider {

    public fun create(
        configuration: DokkaConfiguration,
        logger: DokkaLogger,
    ): DokkaGenerator

    /**
     * The default [DokkaGeneratorProvider] implementation.
     */
    @InternalDokkaApi
    public class Default : DokkaGeneratorProvider {
        override fun create(
            configuration: DokkaConfiguration,
            logger: DokkaLogger,
        ): DokkaGenerator = DokkaGenerator(
            configuration = configuration,
            logger = logger,
        )
    }

    @InternalDokkaApi
    public companion object {
        /**
         * Load [DokkaGeneratorProvider.Default] using [ServiceLoader].
         *
         * Requires only one implementation of [DokkaGeneratorProvider] is registered.
         *
         * @throws IllegalStateException if more than one [DokkaGeneratorProvider] is available.
         */
        @InternalDokkaApi
        public fun loadDefault(classLoader: ClassLoader): DokkaGeneratorProvider {
            val cls = DokkaGeneratorProvider::class
            val implementations = ServiceLoader.load(cls.java, classLoader)
            return implementations.singleOrNull()
                ?: error("Expected one implementation for ${cls.qualifiedName}, but found ${implementations.map { it::class.qualifiedName }}")
        }

    }
}
