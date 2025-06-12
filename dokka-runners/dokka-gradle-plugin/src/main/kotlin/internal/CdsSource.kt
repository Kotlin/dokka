/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.internal

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.konan.file.use
import java.io.File
import java.io.OutputStream
import java.io.RandomAccessFile
import java.math.BigInteger
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import java.nio.file.Files
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import java.util.jar.JarFile
import javax.inject.Inject
import kotlin.concurrent.withLock


internal abstract class CdsSource
@Inject
internal constructor(
    private val execOps: ExecOperations
) : ValueSource<File, CdsSource.Parameters> {

    interface Parameters : ValueSourceParameters {
        val classpath: ConfigurableFileCollection
    }

    private val classpathChecksum: String by lazy {
        checksum(parameters.classpath)
    }

    private val cacheDir: File by lazy {
        val osName = System.getProperty("os.name").lowercase()
        val homeDir = System.getProperty("user.home")
        val appDataDir = System.getenv("APP_DATA") ?: homeDir

        val userCacheDir = when {
            "win" in osName -> "$appDataDir/Caches/"
            "mac" in osName -> "$homeDir/Library/Caches/"
            "nix" in osName -> "$homeDir/.cache/"
            else -> "$homeDir/.cache/"
        }

        File(userCacheDir).resolve("dokka").apply {
            mkdirs()
        }
    }

    private val cdsFile: File by lazy {
        cacheDir.resolve("$classpathChecksum.jsa")
    }
    private val lockFile: File by lazy {
        cacheDir.resolve("$classpathChecksum.lock")
    }

    override fun obtain(): File {
        lock.withLock {
            RandomAccessFile(lockFile, "rw").use {
                it.channel.lockWithRetries().use {
                    if (!cdsFile.exists()) {
                        generateStaticCds()
                    }
                    println("Using CDS ${cdsFile.absoluteFile.invariantSeparatorsPath}")
                    return cdsFile
                }
            }
        }
    }

    private fun generateStaticCds() {

        val classListFile = Files.createTempFile("asd", "classlist").toFile()
        parameters.classpath.files.flatMap { file ->
            getClassNamesFromJarFile(file)
        }
            .toSet()
            .joinToString("\n")
            .let {
                classListFile.writeText(it)
            }

        execOps.javaexec {
            jvmArgs(
                "-Xshare:dump",
                "-XX:SharedArchiveFile=${cdsFile.absoluteFile.invariantSeparatorsPath}",
                "-XX:SharedClassListFile=${classListFile.absoluteFile.invariantSeparatorsPath}"
            )
            classpath(parameters.classpath)
        }
    }

    companion object {
        private val lock: Lock = ReentrantLock()
    }
}


private fun checksum(
    files: ConfigurableFileCollection
): String {
    val md = MessageDigest.getInstance("md5")
    DigestOutputStream(nullOutputStream(), md).use { os ->
        os.write(files.asPath.encodeToByteArray())
    }
    return BigInteger(1, md.digest()).toString(16)
        .padStart(md.digestLength * 2, '0')
}

private fun checksum(
    files: Collection<File>
): String {
    val md = MessageDigest.getInstance("md5")
    DigestOutputStream(nullOutputStream(), md).use { os ->
        files.forEach { file ->
            file.inputStream().use { it.copyTo(os) }
        }
    }
    return BigInteger(1, md.digest()).toString(16)
        .padStart(md.digestLength * 2, '0')
}

private fun nullOutputStream(): OutputStream =
    object : OutputStream() {
        override fun write(b: Int) {}
    }


private fun getClassNamesFromJarFile(source: File): Set<String> {
    JarFile(source).use { jarFile ->
        return jarFile.entries().asSequence()
            .filter { it.name.endsWith(".class") }
            .map { entry ->
                entry.name
                    .replace("/", ".")
                    .removeSuffix(".class")
            }
            .toSet()
    }
}

private fun FileChannel.lockWithRetries(): FileLock {
    var retries = 0
    while (true) {
        try {
            return lock()
        }
        /*
        Catching the OverlappingFileLockException which is caused by the same jvm (process) already having locked the file.
        Since we do use a static re-entrant lock as a monitor to the cache, this can only happen
        when this code is running in the same JVM but with in complete isolation
        (e.g. Gradle classpath isolation, or composite builds).

        If we detect this case, we retry the locking after a short period, constantly logging that we're blocked
        by some other thread using the cache.

        The risk of deadlocking here is low, since we can only get into this code path, *if*
        the code is very isolated and somebody locked the file.
         */
        catch (t: OverlappingFileLockException) {
            Thread.sleep(25)
            retries++
//            if (retries % 10 == 0) {
////                logInfo("Waiting to acquire lock: $file")
//            }
        }
    }
}
