package org.jetbrains.dokka.javadoc

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.plugability.DokkaPlugin

class JavadocPlugin : DokkaPlugin() {
   val dokkaJavadocPlugin by extending { CoreExtensions.renderer with JavadocRenderer() }
}

