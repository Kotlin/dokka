/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import dokkabuild.utils.declarable
import dokkabuild.utils.jvmJar
import dokkabuild.utils.resolvable

/**
 * Utility to run unit tests for K1 and K2 (analysis API).
 */

plugins {
    id("dokkabuild.base")
    id("dokkabuild.java")
    `jvm-test-suite`
}

val symbolsTestImplementation: Configuration by configurations.creating {
    description = "Dependencies for symbols tests"
    declarable()
}

val symbolsTestImplementationResolver: Configuration by configurations.creating {
    description = "Resolve dependencies for symbols tests"
    resolvable()
    extendsFrom(symbolsTestImplementation)
    attributes { jvmJar(objects) }
}

val descriptorsTestImplementation: Configuration by configurations.creating {
    description = "Dependencies for descriptors tests"
    declarable()
}

val descriptorsTestImplementationResolver: Configuration by configurations.creating {
    description = "Resolve dependencies for descriptors tests"
    resolvable()
    extendsFrom(descriptorsTestImplementation)
    attributes { jvmJar(objects) }
}

@Suppress("UnstableApiUsage")
testing {
    suites {
        val test by suites.getting(JvmTestSuite::class) {

            val descriptorTags = listOf("onlyDescriptors", "onlyDescriptorsMPP")
            val symbolsTags = listOf("onlySymbols")

            targets.named("test") {
                testTask.configure {
                    description = "Runs tests (excluding descriptor and symbols tags: ${descriptorTags + symbolsTags})"
                    useJUnitPlatform {
                        excludeTags.addAll(descriptorTags + symbolsTags)
                    }
                    classpath += descriptorsTestImplementationResolver.incoming.files
                }
            }

            targets.register("descriptorsTest") {
                testTask.configure {
                    description = "Runs all descriptor tests (tags: ${descriptorTags})"
                    useJUnitPlatform {
                        includeTags.addAll(descriptorTags)
                    }
                    classpath += descriptorsTestImplementationResolver.incoming.files
                }
            }

            targets.register("symbolsTest") {
                testTask.configure {
                    description = "Runs all symbols tests (tags: ${symbolsTags})"
                    useJUnitPlatform {
                        includeTags.addAll(symbolsTags)
                    }
                    classpath += symbolsTestImplementationResolver.incoming.files
                }
            }
        }
    }
}
