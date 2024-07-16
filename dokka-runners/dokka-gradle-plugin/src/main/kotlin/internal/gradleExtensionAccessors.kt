package dev.adamko.dokkatoo.internal

import dev.adamko.dokkatoo.DokkatooExtension

// When Dokkatoo is applied to a build script Gradle will auto-generate these accessors

internal fun DokkatooExtension.versions(configure: DokkatooExtension.Versions.() -> Unit) {
  versions.apply(configure)
}
