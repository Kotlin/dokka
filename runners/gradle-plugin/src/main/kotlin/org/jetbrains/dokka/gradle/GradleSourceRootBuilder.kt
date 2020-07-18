package org.jetbrains.dokka.gradle

import org.gradle.api.tasks.InputDirectory
import org.jetbrains.dokka.DokkaConfigurationBuilder
import org.jetbrains.dokka.SourceRootImpl
import java.io.File

class GradleSourceRootBuilder : DokkaConfigurationBuilder<SourceRootImpl> {
    @InputDirectory
    var directory: File? = null

    override fun build(): SourceRootImpl {
        return SourceRootImpl(checkNotNull(directory) { "directory not set" })
    }
}
