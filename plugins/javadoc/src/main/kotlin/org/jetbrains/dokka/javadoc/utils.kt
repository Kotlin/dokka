package org.jetbrains.dokka.javadoc

import java.nio.file.Path
import java.nio.file.Paths

internal fun Path.toNormalized() = this.normalize().toFile().toString()

internal fun String.toNormalized() = Paths.get(this).toNormalized()
