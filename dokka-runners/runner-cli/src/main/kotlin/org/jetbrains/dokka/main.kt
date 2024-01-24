/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka

import org.jetbrains.dokka.DokkaConfiguration.ExternalDocumentationLink
import org.jetbrains.dokka.utilities.*
import java.nio.file.Paths

public fun main(args: Array<String>) {
    val globalArguments = GlobalArguments(args)
    val configuration = initializeConfiguration(globalArguments)
    DokkaGenerator(configuration, globalArguments.logger).generate()
}

public fun initializeConfiguration(globalArguments: GlobalArguments): DokkaConfiguration {
    return if (globalArguments.json != null) {
        val jsonContent = Paths.get(checkNotNull(globalArguments.json)).toFile().readText()
        val globals = GlobalDokkaConfiguration(jsonContent)
        val dokkaConfigurationImpl = DokkaConfigurationImpl(jsonContent)

        dokkaConfigurationImpl.apply(globals).apply {
            sourceSets.forEach {
                it.externalDocumentationLinks.cast<MutableSet<ExternalDocumentationLink>>().addAll(defaultLinks(it))
            }
        }
    } else {
        globalArguments
    }
}

