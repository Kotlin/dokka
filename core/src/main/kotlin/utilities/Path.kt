package org.jetbrains.dokka.utilities

import java.io.File

fun File.appendExtension(extension: String) = if (extension.isEmpty()) this else File(path + "." + extension)
