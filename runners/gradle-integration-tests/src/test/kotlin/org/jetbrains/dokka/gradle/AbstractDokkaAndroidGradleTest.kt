package org.jetbrains.dokka.gradle

import org.junit.BeforeClass
import java.io.File

abstract class AbstractDokkaAndroidGradleTest : AbstractDokkaGradleTest() {

    override val pluginClasspath: List<File> = androidPluginClasspathData.toFile().readLines().map { File(it) }

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

            if (System.getProperty("android.licenses.accept", "false") != "true") return

            if (!sdkLicensesDir.exists()) sdkLicensesDir.mkdir()

            val process = ProcessBuilder(sdkDirFile.resolve("tools/bin/sdkmanager").toString(), "--licenses")
                .inheritIO()
                .redirectInput(ProcessBuilder.Redirect.PIPE)
                .start()

            val processIn = process.outputStream.bufferedWriter()


            while (process.isAlive) {
                processIn.write("y")
                processIn.newLine()
            }

            processIn.close()
            require(process.exitValue() == 0) { "sdkmanager exited with non-zero value" }

        }

    }
}