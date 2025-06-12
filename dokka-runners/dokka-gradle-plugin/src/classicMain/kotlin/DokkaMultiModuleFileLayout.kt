/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle

import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.jetbrains.dokka.DokkaException
import java.io.File

/**
 * @see DokkaMultiModuleFileLayout.targetChildOutputDirectory
 * @see org.jetbrains.dokka.gradle.DokkaMultiModuleFileLayout.NoCopy
 * @see org.jetbrains.dokka.gradle.DokkaMultiModuleFileLayout.CompactInParent
 */
@Deprecated(DOKKA_V1_DEPRECATION_MESSAGE)
fun interface DokkaMultiModuleFileLayout {

    /**
     * @param parent: The [DokkaMultiModuleTask] that is initiating a composite documentation run
     * @param child: Some child task registered in [parent]
     * @return The target output directory of the [child] dokka task referenced by [parent]. This should
     * be unique for all registered child tasks.
     */
    fun targetChildOutputDirectory(
        parent: @Suppress("DEPRECATION") DokkaMultiModuleTask,
        child: @Suppress("DEPRECATION") AbstractDokkaTask
    ): Provider<Directory>

    /**
     * Will link to the original [AbstractDokkaTask.outputDirectory]. This requires no copying of the output files.
     */
    @Deprecated(DOKKA_V1_DEPRECATION_MESSAGE)
    object NoCopy : @Suppress("DEPRECATION") DokkaMultiModuleFileLayout {
        override fun targetChildOutputDirectory(
            parent: @Suppress("DEPRECATION") DokkaMultiModuleTask,
            child: @Suppress("DEPRECATION") AbstractDokkaTask
        ): Provider<Directory> = child.outputDirectory
    }

    /**
     * Will point to a subfolder inside the output directory of the parent.
     * The subfolder will follow the structure of the gradle project structure
     * e.g.
     * :parentProject:firstAncestor:secondAncestor will be be resolved to
     * {parent output directory}/firstAncestor/secondAncestor
     */
    @Deprecated(DOKKA_V1_DEPRECATION_MESSAGE)
    object CompactInParent : @Suppress("DEPRECATION") DokkaMultiModuleFileLayout {
        override fun targetChildOutputDirectory(
            parent: @Suppress("DEPRECATION") DokkaMultiModuleTask,
            child: @Suppress("DEPRECATION") AbstractDokkaTask
        ): Provider<Directory> {
            val relativeProjectPath = parent.project.relativeProjectPath(child.project.path)
            val relativeFilePath = relativeProjectPath.replace(":", File.separator)
            check(!File(relativeFilePath).isAbsolute) { "Unexpected absolute path $relativeFilePath" }
            return parent.outputDirectory.dir(relativeFilePath)
        }
    }
}

internal fun @Suppress("DEPRECATION") DokkaMultiModuleTask.targetChildOutputDirectory(
    child: @Suppress("DEPRECATION") AbstractDokkaTask
): Provider<Directory> = fileLayout.get().targetChildOutputDirectory(this, child)


internal fun @Suppress("DEPRECATION") DokkaMultiModuleTask.copyChildOutputDirectory(child: @Suppress("DEPRECATION") AbstractDokkaTask) {
    val targetChildOutputDirectory = project.file(fileLayout.get().targetChildOutputDirectory(this, child))
    val sourceChildOutputDirectory = child.outputDirectory.asFile.get()

    /* Pointing to the same directory -> No copy necessary */
    if (sourceChildOutputDirectory.absoluteFile == targetChildOutputDirectory.absoluteFile) {
        return
    }

    /* Cannot target *inside* the original folder */
    if (targetChildOutputDirectory.absoluteFile.startsWith(sourceChildOutputDirectory.absoluteFile)) {
        throw DokkaException(
            "Cannot re-locate output directory into itself.\n" +
                    "sourceChildOutputDirectory=${sourceChildOutputDirectory.path}\n" +
                    "targetChildOutputDirectory=${targetChildOutputDirectory.path}"
        )
    }

    /* Source output directory is empty -> No copy necessary */
    if (!sourceChildOutputDirectory.exists()) {
        return
    }

    sourceChildOutputDirectory.copyRecursively(targetChildOutputDirectory, overwrite = true)
}
