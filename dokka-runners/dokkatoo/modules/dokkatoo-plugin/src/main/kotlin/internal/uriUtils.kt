package org.jetbrains.dokka.dokkatoo.internal

import java.net.URI

internal fun URI.appendPath(addition: String): URI {
  val currentPath = path.removeSuffix("/")
  val newPath = "$currentPath/$addition"
  return resolve(newPath).normalize()
}
