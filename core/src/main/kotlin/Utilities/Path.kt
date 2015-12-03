package org.jetbrains.dokka

import java.io.File

fun File.appendExtension(extension: String) = if (extension.isEmpty()) this else File(path + "." + extension)
