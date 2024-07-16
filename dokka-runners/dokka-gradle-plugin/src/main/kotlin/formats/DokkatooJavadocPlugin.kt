package dev.adamko.dokkatoo.formats

import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import org.gradle.kotlin.dsl.*

abstract class DokkatooJavadocPlugin
@DokkatooInternalApi
constructor() : DokkatooFormatPlugin(formatName = "javadoc") {
  override fun DokkatooFormatPluginContext.configure() {
    project.dependencies {
      dokkaPlugin(dokka("javadoc-plugin"))
    }
  }
}
