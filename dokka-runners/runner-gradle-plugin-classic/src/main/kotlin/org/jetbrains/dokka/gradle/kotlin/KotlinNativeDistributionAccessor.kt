@file:Suppress("INVISIBLE_REFERENCE")
package org.jetbrains.dokka.gradle.kotlin

import java.io.File
import org.gradle.api.Project
import org.jetbrains.kotlin.commonizer.KonanDistribution
import org.jetbrains.kotlin.commonizer.platformLibsDir
import org.jetbrains.kotlin.commonizer.stdlib
import org.jetbrains.kotlin.compilerRunner.konanHome
import org.jetbrains.kotlin.konan.target.KonanTarget

/**
 * Provides access to the Kotlin/Native distribution components:
 * * [stdlibDir] -- stdlib directory
 * * [platformDependencies] -- list of directories to platform dependencies
 *
 * It uses Kotlin Gradle Plugin API that is guaranteed to be present in:
 *  1.5 <= kotlinVersion <= 1.9
 *
 * It should not be used with Kotlin versions later than 1.9
 */
internal class KotlinNativeDistributionAccessor(
  project: Project
) {
  private val konanDistribution = KonanDistribution(
    @Suppress("INVISIBLE_MEMBER")
    project.konanHome
  )

  val stdlibDir: File = konanDistribution.stdlib

  fun platformDependencies(target: KonanTarget): List<File> = konanDistribution
    .platformLibsDir
    .resolve(target.name)
    .listLibraryFiles()

  private fun File.listLibraryFiles(): List<File> = listFiles().orEmpty()
    .filter { it.isDirectory || it.extension == "klib" }
}