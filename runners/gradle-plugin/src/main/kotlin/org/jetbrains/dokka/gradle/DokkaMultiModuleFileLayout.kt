package org.jetbrains.dokka.gradle

import java.io.File

interface DokkaMultiModuleFileLayout {
    fun targetChildOutputDirectory(parent: AbstractDokkaParentTask, child: AbstractDokkaTask): File

    object NoCopy : DokkaMultiModuleFileLayout {
        override fun targetChildOutputDirectory(parent: AbstractDokkaParentTask, child: AbstractDokkaTask): File {
            return child.outputDirectory.getSafe()
        }
    }

    object CompactInParent : DokkaMultiModuleFileLayout {
        override fun targetChildOutputDirectory(parent: AbstractDokkaParentTask, child: AbstractDokkaTask): File {
            val relativeProjectPath = parent.project.relativeProjectPath(child.project.path)
            val relativeFilePath = relativeProjectPath.replace(":", File.separator)
            check(!File(relativeFilePath).isAbsolute) { "Unexpected absolute path $relativeFilePath" }
            return parent.outputDirectory.getSafe().resolve(relativeFilePath)
        }
    }
}

internal fun DokkaMultiModuleTask.targetChildOutputDirectory(
    child: AbstractDokkaTask
): File {
    return fileLayout.targetChildOutputDirectory(this, child)
}

internal fun DokkaMultiModuleTask.copyChildOutputDirectories() {
    childDokkaTasks.forEach { child ->
        fileLayout.copyChildOutputDirectory(this, child)
    }
}

internal fun DokkaMultiModuleFileLayout.copyChildOutputDirectory(
    parent: AbstractDokkaParentTask, child: AbstractDokkaTask
) {
    val targetChildOutputDirectory = parent.project.file(targetChildOutputDirectory(parent, child))
    val sourceChildOutputDirectory = child.outputDirectory.getSafe()

    if (!sourceChildOutputDirectory.exists()) {
        return
    }

    if (sourceChildOutputDirectory.absoluteFile == targetChildOutputDirectory.absoluteFile) {
        return
    }

    sourceChildOutputDirectory.copyRecursively(targetChildOutputDirectory, overwrite = false)
}

