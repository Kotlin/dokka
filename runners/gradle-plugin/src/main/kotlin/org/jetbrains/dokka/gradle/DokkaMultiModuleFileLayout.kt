package org.jetbrains.dokka.gradle

import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.jetbrains.dokka.DokkaException
import org.jetbrains.dokka.gradle.DokkaMultiModuleFileLayout.CompactInParent
import org.jetbrains.dokka.gradle.DokkaMultiModuleFileLayout.NoCopy
import java.io.File

/**
 * @see DokkaMultiModuleFileLayout.targetChildOutputDirectory
 * @see NoCopy
 * @see CompactInParent
 */
fun interface DokkaMultiModuleFileLayout {

    /**
     * @param parent: The [DokkaMultiModuleTask] that is initiating a composite documentation run
     * @param child: Some child task registered in [parent]
     * @return The target output directory of the [child] dokka task referenced by [parent]. This should
     * be unique for all registered child tasks.
     */
    fun targetChildOutputDirectory(parent: DokkaMultiModuleTask, child: AbstractDokkaTask): Provider<Directory>

    /**
     * Will link to the original [AbstractDokkaTask.outputDirectory]. This requires no copying of the output files.
     */
    object NoCopy : DokkaMultiModuleFileLayout {
        override fun targetChildOutputDirectory(
            parent: DokkaMultiModuleTask,
            child: AbstractDokkaTask
        ): Provider<Directory> = child.outputDirectory
    }

    /**
     * Will point to a subfolder inside the output directory of the parent.
     * The subfolder will follow the structure of the gradle project structure
     * e.g.
     * :parentProject:firstAncestor:secondAncestor will be be resolved to
     * {parent output directory}/firstAncestor/secondAncestor
     */
    object CompactInParent : DokkaMultiModuleFileLayout {
        override fun targetChildOutputDirectory(
            parent: DokkaMultiModuleTask,
            child: AbstractDokkaTask
        ): Provider<Directory> {
            val relativeProjectPath = parent.project.relativeProjectPath(child.project.path)
            val relativeFilePath = relativeProjectPath.replace(":", File.separator)
            check(!File(relativeFilePath).isAbsolute) { "Unexpected absolute path $relativeFilePath" }
            return parent.outputDirectory.dir(relativeFilePath)
        }
    }
}

internal fun DokkaMultiModuleTask.targetChildOutputDirectory(
    child: AbstractDokkaTask
): Provider<Directory> = fileLayout.get().targetChildOutputDirectory(this, child)


internal fun DokkaMultiModuleTask.copyChildOutputDirectories() {
    childDokkaTasks.forEach { child ->
        this.copyChildOutputDirectory(child)
    }
}

internal fun DokkaMultiModuleTask.copyChildOutputDirectory(child: AbstractDokkaTask) {
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
