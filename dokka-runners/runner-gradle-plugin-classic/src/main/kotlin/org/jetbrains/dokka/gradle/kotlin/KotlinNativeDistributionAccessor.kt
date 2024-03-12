/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("INVISIBLE_REFERENCE")
package org.jetbrains.dokka.gradle.kotlin

import org.gradle.api.Project
import org.jetbrains.kotlin.commonizer.KonanDistribution
import org.jetbrains.kotlin.commonizer.platformLibsDir
import org.jetbrains.kotlin.commonizer.stdlib
import org.jetbrains.kotlin.gradle.utils.NativeCompilerDownloader
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File

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
      // see this comment for the explanation of what's happening:
      // https://github.com/Kotlin/dokka/pull/3516#issuecomment-1992141380
      Class.forName("org.jetbrains.kotlin.compilerRunner.NativeToolRunnersKt")
          .declaredMethods
          .find { it.name == "getKonanHome" && it.returnType.simpleName == "String" }
          ?.invoke(null, project) as? String
          ?: project.alternativeKonanHome()
          ?: error("Unable to find the Kotlin Native home")
  )

  val stdlibDir: File = konanDistribution.stdlib

  private fun Project.alternativeKonanHome(): String? {
      val nativeHome = this.findProperty("org.jetbrains.kotlin.native.home") as? String ?: return null
      return File(nativeHome).absolutePath ?: NativeCompilerDownloader(project).compilerDirectory.absolutePath
  }

  fun platformDependencies(target: KonanTarget): List<File> = konanDistribution
    .platformLibsDir
    .resolve(target.name)
    .listLibraryFiles()

  private fun File.listLibraryFiles(): List<File> = listFiles().orEmpty()
    .filter { it.isDirectory || it.extension == "klib" }
}
