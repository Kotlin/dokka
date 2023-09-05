/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
        maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven") {
            content {
                includeGroup("org.jetbrains.kotlinx")
            }
        }
    }
}

afterEvaluate {
    logger.quiet("Gradle version: ${gradle.gradleVersion}")
    logger.quiet("Kotlin version: ${properties["dokka_it_kotlin_version"]}")
    properties["dokka_it_android_gradle_plugin_version"]?.let { androidVersion ->
        logger.quiet("Android version: $androidVersion")
    }
}
