/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.internal

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.kotlin.dsl.provideDelegate
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
import kotlin.random.Random


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

    private val cdsFile: File by lazy {
        cdsCacheDir.resolve("$classpathChecksum.jsa")
    }
    private val lockFile: File by lazy {
        cdsCacheDir.resolve("$classpathChecksum.lock")
    }

    override fun obtain(): File? {
        if (currentJavaVersion < 17) {
            logger.warn("CDS generation is only supported for Java 17 and above. Current version $currentJavaVersion.")
            return null
        }
        if (cdsFile.exists()) {
            cdsFile.setLastModified(System.currentTimeMillis())
            return cdsFile
        }

        lock.withLock {
            RandomAccessFile(lockFile, "rw").use {
                it.channel.lockLenient().use {
                    if (cdsFile.exists()) {
                        return cdsFile
                    } else {
                        generateStaticCds()
                        logger.warn("Using CDS ${cdsFile.absoluteFile.invariantSeparatorsPath}")
                        return cdsFile
                    }
                }
            }
        }
    }

    private fun generateStaticCds() {

        val classListFile = Files.createTempFile("CdsSource", "classlist").toFile()
        classListFile.deleteOnExit()

        parameters.classpath.files
            .flatMap { file -> getClassNamesFromJarFile(file) }
            .distinct()
            .sorted()
            .joinToString("\n")
            .let {
                classListFile.writeText(it)
            }
        logger.warn("Generating CDS from class list: ${classListFile.absoluteFile.invariantSeparatorsPath}")

        execOps.exec {
            executable("java")
            args(
                "-Xshare:dump",
                "-XX:SharedArchiveFile=${cdsFile.absoluteFile.invariantSeparatorsPath}",
                "-XX:SharedClassListFile=${classListFile.absoluteFile.invariantSeparatorsPath}",
                "-cp",
                parameters.classpath.asPath,
//                "${parameters.classpath.asPath}${File.pathSeparator}/Users/dev/projects/jetbrains/dokka/dokka-runners/runner-cli/build/libs/runner-cli-2.0.20-SNAPSHOT.jar",
//                parameters.classpath.asPath,
//                "org.jetbrains.dokka.MainKt"
            )
            //logger.warn("Generating CDS args: $args")
        }
    }

    companion object {
        private val lock: Lock = ReentrantLock()

        private val logger = Logging.getLogger(CdsSource::class.java)

        private val cdsCacheDir: File by lazy {
            val cdsFromEnv = System.getenv("DOKKA_CDS_CACHE_DIR")

            if (cdsFromEnv != null) {
                File(cdsFromEnv).apply {
                    mkdirs()
                }
            } else {
                val osName = System.getProperty("os.name").lowercase()
                val homeDir = System.getProperty("user.home")
                val appDataDir = System.getenv("APP_DATA") ?: homeDir

                val userCacheDir = when {
                    "win" in osName -> "$appDataDir/Caches/"
                    "mac" in osName -> "$homeDir/Library/Caches/"
                    "nix" in osName -> "$homeDir/.cache/"
                    else -> "$homeDir/.cache/"
                }

                File(userCacheDir).resolve("dokka-cds").apply {
                    mkdirs()
                }
            }
        }
    }
}


private fun checksum(
    files: ConfigurableFileCollection
): String {
    val md = MessageDigest.getInstance("md5")
    DigestOutputStream(nullOutputStream(), md).use { os ->
        os.write(files.asPath.encodeToByteArray())

        files.forEach { file ->
            file.inputStream().use {
                it.copyTo(os)
            }
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

private val currentJavaVersion: Int =
    System.getProperty("java.version")
        .removePrefix("1.")
        .substringBefore(".")
        .toInt()


/**
 * Leniently obtain a [FileLock] for the channel.
 *
 * @throws [InterruptedException] if the current thread is interrupted before the lock can be acquired.
 */
private tailrec fun FileChannel.lockLenient(): FileLock {
    if (Thread.interrupted()) {
        throw InterruptedException("Interrupted while waiting for lock on FileChannel@${this@lockLenient.hashCode()}")
    }

    val lock = try {
        tryLock()
    } catch (_: OverlappingFileLockException) {
        // ignore exception - it means the lock is already held by this process.
        null
    }

    if (lock != null) {
        return lock
    }

    try {
        Thread.sleep(Random.nextLong(25, 125))
    } catch (e: InterruptedException) {
        Thread.currentThread().interrupt()
        throw e
    }

    return lockLenient()
}
