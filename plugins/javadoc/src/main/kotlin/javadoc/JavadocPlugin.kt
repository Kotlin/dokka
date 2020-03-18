package org.jetbrains.dokka.javadoc

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.querySingle

class JavadocPlugin : DokkaPlugin() {
    val dokkaJavadocPlugin by extending {
        val dokkaBasePlugin = plugin<DokkaBase>()

        CoreExtensions.renderer providing { ctx ->
            JavadocRenderer(dokkaBasePlugin.querySingle { outputWriter }, ctx)
        }
    }
}

