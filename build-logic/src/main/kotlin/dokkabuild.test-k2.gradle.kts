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

            targets.named("test") {
                testTask.configure {
                    useJUnitPlatform {
                        excludeTags("onlySymbols")
                    }
                    classpath += descriptorsTestImplementationResolver.incoming.files
                }
            }

            targets.register("symbolsTest") {
                testTask.configure {
                    useJUnitPlatform {
                        excludeTags("onlyDescriptors", "onlyDescriptorsMPP")
                    }
                    classpath += symbolsTestImplementationResolver.incoming.files
                }
            }

            // isn't this just the same as the default Test target?
//            targets.register("descriptorsTest") {
//                testTask.configure {
//                    useJUnitPlatform {
//                        excludeTags("onlySymbols")
//                    }
//                    classpath += descriptorsTestImplementationResolver.incoming.files
//                }
//            }
        }
    }
}
