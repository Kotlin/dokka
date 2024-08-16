/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

/**
 * Base configuration for Java/JVM projects.
 */

plugins {
    id("dokkabuild.base")
    java
}

java {
    toolchain {
        languageVersion = dokkaBuild.mainJavaVersion
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()

    maxHeapSize = "1G"
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)

    javaLauncher = javaToolchains.launcherFor {
        languageVersion = dokkaBuild.testJavaLauncherVersion
    }
}

dependencies {
    testImplementation(platform(libs.junit.bom))
}

tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.FAIL
}
