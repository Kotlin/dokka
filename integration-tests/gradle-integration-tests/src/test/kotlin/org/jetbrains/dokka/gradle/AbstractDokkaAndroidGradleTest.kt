package org.jetbrains.dokka.gradle

import org.junit.BeforeClass
import java.io.File

abstract class AbstractDokkaAndroidGradleTest : AbstractDokkaGradleTest() {

    override val pluginClasspath: List<File> = pluginClasspathData.toFile().readLines().map { File(it) }

    companion object {

        @JvmStatic
        @BeforeClass
        fun acceptAndroidSdkLicenses() {
            val sdkDir = androidLocalProperties?.toFile()?.let {
                val lines = it.readLines().map { it.trim() }
                val sdkDirLine = lines.firstOrNull { "sdk.dir" in it }
                sdkDirLine?.substringAfter("=")?.trim()
            } ?: System.getenv("ANDROID_HOME")

            if (sdkDir == null || sdkDir.isEmpty()) {
                error("Android SDK home not set, " +
                        "try setting \$ANDROID_HOME " +
                        "or sdk.dir in runners/gradle-integration-tests/testData/android.local.properties")
            }
            val sdkDirFile = File(sdkDir)
            if (!sdkDirFile.exists()) error("\$ANDROID_HOME and android.local.properties points to non-existing location")
            val sdkLicensesDir = sdkDirFile.resolve("licenses")

            val acceptedLicenses = File("android-licenses")
            acceptedLicenses.listFiles().forEach { licenseFile ->
                val target = sdkLicensesDir.resolve(licenseFile.name)
                if(!target.exists() || target.readText() != licenseFile.readText()) {
                    val overwrite = System.getProperty("android.licenses.overwrite", "false")!!.toBoolean()
                    if (!target.exists() || overwrite) {
                        licenseFile.copyTo(target, true)
                        println("Accepted ${licenseFile.name}, by copying $licenseFile to $target")
                    }
                }

            }
        }

    }
}