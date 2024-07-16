package dev.adamko.dokkatoo.formats

import dev.adamko.dokkatoo.dokka.plugins.DokkaHtmlPluginParameters
import dev.adamko.dokkatoo.dokka.plugins.DokkaHtmlPluginParameters.Companion.DOKKA_HTML_PARAMETERS_NAME
import dev.adamko.dokkatoo.dokka.plugins.DokkaVersioningPluginParameters
import dev.adamko.dokkatoo.dokka.plugins.DokkaVersioningPluginParameters.Companion.DOKKA_VERSIONING_PLUGIN_PARAMETERS_NAME
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import dev.adamko.dokkatoo.internal.uppercaseFirstChar
import dev.adamko.dokkatoo.tasks.DokkatooGeneratePublicationTask
import dev.adamko.dokkatoo.tasks.LogHtmlPublicationLinkTask
import java.io.File
import javax.inject.Inject
import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*

abstract class DokkatooHtmlPlugin
@DokkatooInternalApi
@Inject
constructor(
  archives: ArchiveOperations,
  providers: ProviderFactory,
) : DokkatooFormatPlugin(formatName = "html") {

  private val moduleAggregationCheck: HtmlModuleAggregationCheck =
    HtmlModuleAggregationCheck(archives, providers)

  override fun DokkatooFormatPluginContext.configure() {
    registerDokkaBasePluginConfiguration()
    registerDokkaVersioningPlugin()
    configureHtmlUrlLogging()
    configureModuleAggregation()
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

  /** register and configure Dokka Versioning Plugin */
  private fun DokkatooFormatPluginContext.registerDokkaVersioningPlugin() {
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

  private fun DokkatooFormatPluginContext.configureHtmlUrlLogging() {
    val logHtmlUrlTask = registerLogHtmlUrlTask()

    dokkatooTasks.generatePublication.configure {
      finalizedBy(logHtmlUrlTask)
    }
  }

  private fun DokkatooFormatPluginContext.registerLogHtmlUrlTask():
      TaskProvider<LogHtmlPublicationLinkTask> {

    val generatePublicationTask = dokkatooTasks.generatePublication

    val indexHtmlFile = generatePublicationTask
      .flatMap { it.outputDirectory.file("index.html") }

    val indexHtmlPath = indexHtmlFile.map { indexHtml ->
      indexHtml.asFile
        .relativeTo(project.rootDir.parentFile)
        .invariantSeparatorsPath
    }

    return project.tasks.register<LogHtmlPublicationLinkTask>(
      "logLink" + generatePublicationTask.name.uppercaseFirstChar()
    ) {
      // default port of IntelliJ built-in server is defined in the docs
      // https://www.jetbrains.com/help/idea/settings-debugger.html#24aabda8
      serverUri.convention("http://localhost:63342")
      this.indexHtmlPath.convention(indexHtmlPath)
    }
  }

  /**
   * - Automatically depend on `all-modules-page-plugin` if aggregating multiple projects.
   * - Add a check that logs a warning if `all-modules-page-plugin` is not defined.
   */
  private fun DokkatooFormatPluginContext.configureModuleAggregation() {

    dokkatooTasks.generatePublication.configure {
      doFirst("check all-modules-page-plugin is present", moduleAggregationCheck)
    }

    formatDependencies.dokkaPublicationPluginClasspathApiOnly.configure {
      dependencies.addLater(dokkatooExtension.versions.jetbrainsDokka.map { v ->
        project.dependencies.create("org.jetbrains.dokka:all-modules-page-plugin:$v")
      })
    }
  }

  /**
   * Log a warning if the publication has 1+ modules but `all-modules-page-plugin` is not present,
   * because otherwise Dokka happily runs and produces no output, which is baffling and unhelpful.
   */
  private class HtmlModuleAggregationCheck(
    private val archives: ArchiveOperations,
    private val providers: ProviderFactory,
  ) : Action<Task> {

    private val checkEnabled: Boolean
      get() = providers
        .gradleProperty(HTML_MODULE_AGGREGATION_CHECK_ENABLED)
        .map(String::toBoolean)
        .getOrElse(true)

    override fun execute(task: Task) {
      if (!checkEnabled) {
        logger.info("[${task.path} ModuleAggregationCheck] check is disabled")
        return
      }

      require(task is DokkatooGeneratePublicationTask) {
        "[${task.path} ModuleAggregationCheck] expected DokkatooGeneratePublicationTask but got ${task::class}"
      }

      val modulesCount = task.generator.moduleOutputDirectories.count()

      if (modulesCount <= 0) {
        logger.info("[${task.path} ModuleAggregationCheck] skipping check - publication does not have 1+ modules")
        return
      }

      val allDokkaPlugins = task.generator.pluginsClasspath
        .flatMap { file ->
          extractDokkaPluginMarkers(archives, file)
        }

      val allModulesPagePluginPresent = allDokkaPlugins.any { ALL_MODULES_PAGE_PLUGIN_FQN in it }
      logger.info("[${task.path} ModuleAggregationCheck] allModulesPagePluginPresent:$allModulesPagePluginPresent")

      if (!allModulesPagePluginPresent) {
        val moduleName = task.generator.moduleName.get()

        logger.warn(/* language=text */ """
            |[${task.path}] org.jetbrains.dokka:all-modules-page-plugin is missing.
            |
            |Publication '$moduleName' in has $modulesCount modules, but
            |the Dokka Generator plugins classpath does not contain 
            |   org.jetbrains.dokka:all-modules-page-plugin
            |which is required for aggregating Dokka HTML modules.
            |
            |Dokkatoo should have added org.jetbrains.dokka:all-modules-page-plugin automatically.
            |
            |Generation will proceed, but the generated output might not contain the full HTML docs.
            |
            |Suggestions:
            | - Verify that the dependency has not been excluded.
            | - Raise an issue https://github.com/adamko-dev/dokkatoo/issues
            |
            |(all plugins: ${allDokkaPlugins.sorted().joinToString()})
          """
          .trimMargin()
          .prependIndent("> ")
        )
      }
    }
  }

  @DokkatooInternalApi
  companion object {
    private val logger = Logging.getLogger(DokkatooHtmlPlugin::class.java)

    private const val ALL_MODULES_PAGE_PLUGIN_FQN =
      "org.jetbrains.dokka.allModulesPage.AllModulesPagePlugin"

    private const val HTML_MODULE_AGGREGATION_CHECK_ENABLED =
      "dev.adamko.dokkatoo.tasks.html.moduleAggregationCheckEnabled"

    private const val DOKKA_PLUGIN_MARKER_PATH =
      "/META-INF/services/org.jetbrains.dokka.plugability.DokkaPlugin"

    internal fun extractDokkaPluginMarkers(archives: ArchiveOperations, file: File): List<String> {
      val markers = archives.zipTree(file)
        .matching { include(DOKKA_PLUGIN_MARKER_PATH) }

      val pluginIds = markers.flatMap { marker ->
        marker.useLines { lines ->
          lines
            .filter { line -> line.isNotBlank() && !line.startsWith("#") }
            .toList()
        }
      }

      return pluginIds
    }
  }
}
