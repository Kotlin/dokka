package org.jetbrains.dokka.it

import java.io.File

/**
 * Indicating whether or not the current machine executing the test is a CI
 */
val isCI: Boolean get() = System.getenv("CI") == "true"

val isAndroidSdkInstalled: Boolean = System.getenv("ANDROID_SDK_ROOT") != null ||
        System.getenv("ANDROID_HOME") != null

val isMavenInstalled: Boolean = System.getenv("PATH").orEmpty()
    .split(File.pathSeparator)
    .flatMap { pathElement -> File(pathElement).listFiles().orEmpty().toList() }
    .any { pathElement -> "mvn" == pathElement.name }
