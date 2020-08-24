package org.jetbrains.dokka.test

/**
 * Indicating whether or not the current machine executing the test is a CI
 */
val isCI: Boolean get() = System.getenv("CI") == "true"

val isAndroidSdkInstalled: Boolean = System.getenv("ANDROID_SDK_ROOT") != null ||
        System.getenv("ANDROID_HOME") != null
