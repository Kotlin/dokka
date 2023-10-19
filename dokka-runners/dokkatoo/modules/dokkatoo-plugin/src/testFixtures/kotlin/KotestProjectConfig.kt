package org.jetbrains.dokka.dokkatoo.utils

import io.kotest.core.config.AbstractProjectConfig

@Suppress("unused") // this class is automatically picked up by Kotest
object KotestProjectConfig : AbstractProjectConfig() {
  init {
    displayFullTestPath = true
  }
}
