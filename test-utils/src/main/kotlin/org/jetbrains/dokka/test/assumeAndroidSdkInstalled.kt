package org.jetbrains.dokka.test

import org.junit.Assume.assumeTrue

fun assumeAndroidSdkInstalled() {
    if (isCI) return
    assumeTrue(isAndroidSdkInstalled)
}
