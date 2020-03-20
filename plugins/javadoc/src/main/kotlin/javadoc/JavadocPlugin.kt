package org.jetbrains.dokka.javadoc

import javadoc.pages.PackageSummaryInstaller
import javadoc.pages.ResourcesInstaller
import javadoc.pages.RootCreator
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

    val rootCreator by extending {
        CoreExtensions.pageTransformer with RootCreator
    }
    val packageSummaryInstaller by extending {
        CoreExtensions.pageTransformer with PackageSummaryInstaller order { after(rootCreator) }
    }
    val resourcesInstaller by extending {
        CoreExtensions.pageTransformer with ResourcesInstaller order { after(rootCreator) }
    }
}

