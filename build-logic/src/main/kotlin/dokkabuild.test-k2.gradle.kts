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
    description = "Dependencies for symbols tests (K2)"
    declarable()
}

val symbolsTestImplementationResolver: Configuration by configurations.creating {
    description = "Resolve dependencies for symbols tests (K2)"
    resolvable()
    extendsFrom(symbolsTestImplementation)
    attributes { jvmJar(objects) }
}

val descriptorsTestImplementation: Configuration by configurations.creating {
    description = "Dependencies for descriptors tests (K1)"
    declarable()
}

val descriptorsTestImplementationResolver: Configuration by configurations.creating {
    description = "Resolve dependencies for descriptors tests (K1)"
    resolvable()
    extendsFrom(descriptorsTestImplementation)
    attributes { jvmJar(objects) }
}

@Suppress("UnstableApiUsage")
testing {
    suites {
        val test by suites.getting(JvmTestSuite::class) {

            // JUnit tags for descriptors (K1) and symbols (K2) are defined with annotations in test classes.
            val onlyDescriptorTags = listOf("onlyDescriptors", "onlyDescriptorsMPP")
            val onlySymbolsTags = listOf("onlySymbols")

            // Modify the regular :test target to exclude symbols (K2) tests.
            targets.named("test") {
                testTask.configure {
                    description = "Runs tests using descriptors-analysis (K1) (excluding tags: $onlySymbolsTags)"
                    useJUnitPlatform {
                        excludeTags.addAll(onlySymbolsTags)
                    }
                    classpath += descriptorsTestImplementationResolver.incoming.files
                }
            }

            // Create a new target for _only_ running descriptor (K1) tests.
            targets.register("descriptorsTest") {
                testTask.configure {
                    description = "Runs tests using descriptors-analysis (K1) (excluding tags: ${onlySymbolsTags})"
                    useJUnitPlatform {
                        excludeTags.addAll(onlySymbolsTags)
                    }
                    classpath += descriptorsTestImplementationResolver.incoming.files
                }
            }

            // Create a new target for _only_ running symbols (K2) tests.
            val symbolsTestTarget = targets.register("symbolsTest") {
                testTask.configure {
                    description =
                        "Runs tests using symbols-analysis (K2) (excluding tags: ${onlyDescriptorTags})"
                    useJUnitPlatform {
                        excludeTags.addAll(onlyDescriptorTags)
                    }
                    classpath += symbolsTestImplementationResolver.incoming.files
                }
            }

            // So that both K1 and K2 are tested, when running :check, also run :symbolsTest
            // (Running :descriptorsTest isn't required, because it has the same tags/dependencies as :test)
            tasks.check {
                dependsOn(symbolsTestTarget.map { it.testTask })
            }
        }
    }
}
