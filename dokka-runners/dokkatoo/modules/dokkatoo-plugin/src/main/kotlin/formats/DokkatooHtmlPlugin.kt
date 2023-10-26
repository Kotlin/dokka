package org.jetbrains.dokka.dokkatoo.formats

import org.jetbrains.dokka.dokkatoo.dokka.plugins.DokkaHtmlPluginParameters
import org.jetbrains.dokka.dokkatoo.dokka.plugins.DokkaHtmlPluginParameters.Companion.DOKKA_HTML_PARAMETERS_NAME
import org.jetbrains.dokka.dokkatoo.dokka.plugins.DokkaVersioningPluginParameters
import org.jetbrains.dokka.dokkatoo.dokka.plugins.DokkaVersioningPluginParameters.Companion.DOKKA_VERSIONING_PLUGIN_PARAMETERS_NAME
import org.jetbrains.dokka.dokkatoo.internal.DokkatooInternalApi
import org.jetbrains.dokka.dokkatoo.internal.uppercaseFirstChar
import org.jetbrains.dokka.dokkatoo.tasks.LogHtmlPublicationLinkTask
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*

abstract class DokkatooHtmlPlugin
@DokkatooInternalApi
constructor() : DokkatooFormatPlugin(formatName = "html") {

  override fun DokkatooFormatPluginContext.configure() {
    registerDokkaBasePluginConfiguration()
    registerDokkaVersioningPlugin()

    val logHtmlUrlTask = registerLogHtmlUrlTask()

    dokkatooTasks.generatePublication.configure {
      finalizedBy(logHtmlUrlTask)
    }
  }

  private fun DokkatooFormatPluginContext.registerDokkaBasePluginConfiguration() {
    with(dokkatooExtension.pluginsConfiguration) {
      registerBinding(DokkaHtmlPluginParameters::class, DokkaHtmlPluginParameters::class)
      register<DokkaHtmlPluginParameters>(DOKKA_HTML_PARAMETERS_NAME)
      withType<DokkaHtmlPluginParameters>().configureEach {
        separateInheritedMembers.convention(false)
        mergeImplicitExpectActualDeclarations.convention(false)
      }
    }
  }

  private fun DokkatooFormatPluginContext.registerDokkaVersioningPlugin() {
    // register and configure Dokka Versioning Plugin
    with(dokkatooExtension.pluginsConfiguration) {
      registerBinding(
        DokkaVersioningPluginParameters::class,
        DokkaVersioningPluginParameters::class,
      )
      register<DokkaVersioningPluginParameters>(DOKKA_VERSIONING_PLUGIN_PARAMETERS_NAME)
      withType<DokkaVersioningPluginParameters>().configureEach {
        renderVersionsNavigationOnAllPages.convention(true)
      }
    }
  }

  private fun DokkatooFormatPluginContext.registerLogHtmlUrlTask():
      TaskProvider<LogHtmlPublicationLinkTask> {

    val indexHtmlFile = dokkatooTasks.generatePublication
      .flatMap { it.outputDirectory.file("index.html") }

    val indexHtmlPath = indexHtmlFile.map { indexHtml ->
      indexHtml.asFile
        .relativeTo(project.rootDir.parentFile)
        .invariantSeparatorsPath
    }

    return project.tasks.register<LogHtmlPublicationLinkTask>(
      "logLink" + dokkatooTasks.generatePublication.name.uppercaseFirstChar()
    ) {
      serverUri.convention("http://localhost:63342")
      this.indexHtmlPath.convention(indexHtmlPath)
    }
  }
}
