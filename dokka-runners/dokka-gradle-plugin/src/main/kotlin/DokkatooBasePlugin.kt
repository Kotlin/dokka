package dev.adamko.dokkatoo

import dev.adamko.dokkatoo.dependencies.BaseDependencyManager
import dev.adamko.dokkatoo.dependencies.DependencyContainerNames
import dev.adamko.dokkatoo.dependencies.DokkatooAttribute.Companion.DokkatooClasspathAttribute
import dev.adamko.dokkatoo.dependencies.DokkatooAttribute.Companion.DokkatooFormatAttribute
import dev.adamko.dokkatoo.dependencies.DokkatooAttribute.Companion.DokkatooModuleComponentAttribute
import dev.adamko.dokkatoo.dokka.parameters.DokkaSourceSetSpec
import dev.adamko.dokkatoo.dokka.parameters.KotlinPlatform
import dev.adamko.dokkatoo.dokka.parameters.VisibilityModifier
import dev.adamko.dokkatoo.internal.*
import dev.adamko.dokkatoo.tasks.DokkatooGenerateModuleTask
import dev.adamko.dokkatoo.tasks.DokkatooGenerateTask
import dev.adamko.dokkatoo.tasks.DokkatooTask
import dev.adamko.dokkatoo.tasks.TaskNames
import dev.adamko.dokkatoo.workers.ClassLoaderIsolation
import dev.adamko.dokkatoo.workers.ProcessIsolation
import java.io.File
import javax.inject.Inject
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.*
import org.gradle.language.base.plugins.LifecycleBasePlugin

/**
 * The base plugin for Dokkatoo. Sets up Dokkatoo and configures default values, but does not
 * add any specific config (specifically, it does not create Dokka Publications).
 */
abstract class DokkatooBasePlugin
@DokkatooInternalApi
@Inject
constructor(
  private val providers: ProviderFactory,
  private val layout: ProjectLayout,
  private val objects: ObjectFactory,
) : Plugin<Project> {

  override fun apply(target: Project) {
    // apply the lifecycle-base plugin so the clean task is available
    target.pluginManager.apply(LifecycleBasePlugin::class)

    val dokkatooExtension = createExtension(target)

    configureDependencyAttributes(target)

    configureDokkaPublicationsDefaults(dokkatooExtension)

    initDokkatooTasks(target, dokkatooExtension)
  }

  private fun createExtension(project: Project): DokkatooExtension {

    val baseDependencyManager = BaseDependencyManager(
      project = project,
      objects = objects,
    )

    val dokkatooExtension = project.extensions.create<DokkatooExtension>(
      EXTENSION_NAME,
      baseDependencyManager,
    ).apply {
      moduleName.convention(providers.provider { project.name })
      moduleVersion.convention(providers.provider { project.version.toString() })
      modulePath.convention(project.pathAsFilePath())
      konanHome.convention(
        providers
          .provider {
            // konanHome is set into in extraProperties:
            // https://github.com/JetBrains/kotlin/blob/v1.9.0/libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/native/KotlinNativeTargetPreset.kt#L35-L38
            project.extensions.extraProperties.get("konanHome") as? String?
          }
          .map { File(it) }
      )

      sourceSetScopeDefault.convention(project.path)
      dokkatooPublicationDirectory.convention(layout.buildDirectory.dir("dokka"))
      dokkatooModuleDirectory.convention(layout.buildDirectory.dir("dokka-module"))
      @Suppress("DEPRECATION")
      dokkatooConfigurationsDirectory.convention(layout.buildDirectory.dir("dokka-config"))
    }

    dokkatooExtension.versions {
      jetbrainsDokka.convention(DokkatooConstants.DOKKA_VERSION)
      jetbrainsMarkdown.convention(DokkatooConstants.DOKKA_DEPENDENCY_VERSION_JETBRAINS_MARKDOWN)
      freemarker.convention(DokkatooConstants.DOKKA_DEPENDENCY_VERSION_FREEMARKER)
      kotlinxHtml.convention(DokkatooConstants.DOKKA_DEPENDENCY_VERSION_KOTLINX_HTML)
      kotlinxCoroutines.convention(DokkatooConstants.DOKKA_DEPENDENCY_VERSION_KOTLINX_COROUTINES)
    }

    dokkatooExtension.dokkaGeneratorIsolation.convention(
      dokkatooExtension.ProcessIsolation {
        debug.convention(false)
        jvmArgs.convention(
          listOf(
            //"-XX:MaxMetaspaceSize=512m",
            "-XX:+HeapDumpOnOutOfMemoryError",
            "-XX:+AlwaysPreTouch", // https://github.com/gradle/gradle/issues/3093#issuecomment-387259298
            //"-XX:StartFlightRecording=disk=true,name={path.drop(1).map { if (it.isLetterOrDigit()) it else '-' }.joinToString("")},dumponexit=true,duration=30s",
            //"-XX:FlightRecorderOptions=repository=$baseDir/jfr,stackdepth=512",
          )
        )
      }
    )

    dokkatooExtension.dokkatooSourceSets.configureDefaults(
      sourceSetScopeConvention = dokkatooExtension.sourceSetScopeDefault
    )

    return dokkatooExtension
  }


  private fun configureDependencyAttributes(target: Project) {
    target.dependencies.attributesSchema {
      attribute(DokkatooFormatAttribute)
      attribute(DokkatooModuleComponentAttribute)
      attribute(DokkatooClasspathAttribute)
    }
  }


  /** Set defaults in all [DokkatooExtension.dokkatooPublications]s */
  private fun configureDokkaPublicationsDefaults(
    dokkatooExtension: DokkatooExtension,
  ) {
    dokkatooExtension.dokkatooPublications.all {
      enabled.convention(true)
      cacheRoot.convention(dokkatooExtension.dokkatooCacheDirectory)
      delayTemplateSubstitution.convention(false)
      failOnWarning.convention(false)
      finalizeCoroutines.convention(false)
      moduleName.convention(dokkatooExtension.moduleName)
      moduleVersion.convention(dokkatooExtension.moduleVersion)
      offlineMode.convention(false)
      outputDir.convention(dokkatooExtension.dokkatooPublicationDirectory)
      suppressInheritedMembers.convention(false)
      suppressObviousFunctions.convention(true)
    }
  }


  /** Set conventions for all [DokkaSourceSetSpec] properties */
  private fun NamedDomainObjectContainer<DokkaSourceSetSpec>.configureDefaults(
    sourceSetScopeConvention: Property<String>,
  ) {
    configureEach dss@{
      analysisPlatform.convention(KotlinPlatform.DEFAULT)
      displayName.convention(
        analysisPlatform.map { platform ->
          // Match existing Dokka naming conventions. (This should probably be simplified!)
          when {
            // Multiplatform source sets (e.g. commonMain, jvmMain, macosMain)
            name.endsWith("Main") -> name.substringBeforeLast("Main")

            // indeterminate source sets should use the name of the Kotlin platform
            else                  -> platform.displayName
          }
        }
      )
      documentedVisibilities.convention(setOf(VisibilityModifier.PUBLIC))
      jdkVersion.convention(11)

      enableKotlinStdLibDocumentationLink.convention(true)
      enableJdkDocumentationLink.convention(true)
      enableAndroidDocumentationLink.convention(
        analysisPlatform.map { it == KotlinPlatform.AndroidJVM }
      )

      reportUndocumented.convention(false)
      skipDeprecated.convention(false)
      skipEmptyPackages.convention(true)
      sourceSetScope.convention(sourceSetScopeConvention)

      // Manually added sourceSets should not be suppressed by default. dokkatooSourceSets that are
      // automatically added by DokkatooKotlinAdapter will have a sensible value for 'suppress'.
      suppress.convention(false)

      suppressGeneratedFiles.convention(true)

      sourceLinks.configureEach {
        localDirectory.convention(layout.projectDirectory)
        remoteLineSuffix.convention("#L")
      }

      perPackageOptions.configureEach {
        matchingRegex.convention(".*")
        suppress.convention(false)
        skipDeprecated.convention(false)
        reportUndocumented.convention(false)
      }

      externalDocumentationLinks {
        configureEach {
          enabled.convention(true)
          packageListUrl.convention(url.map { it.appendPath("package-list") })
        }

        maybeCreate("jdk") {
          enabled.convention(this@dss.enableJdkDocumentationLink)
          url(this@dss.jdkVersion.map { jdkVersion ->
            when {
              jdkVersion < 11 -> "https://docs.oracle.com/javase/${jdkVersion}/docs/api/"
              else            -> "https://docs.oracle.com/en/java/javase/${jdkVersion}/docs/api/"
            }
          })
          packageListUrl(this@dss.jdkVersion.map { jdkVersion ->
            when {
              jdkVersion < 11 -> "https://docs.oracle.com/javase/${jdkVersion}/docs/api/package-list"
              else            -> "https://docs.oracle.com/en/java/javase/${jdkVersion}/docs/api/element-list"
            }
          })
        }

        maybeCreate("kotlinStdlib") {
          enabled.convention(this@dss.enableKotlinStdLibDocumentationLink)
          url("https://kotlinlang.org/api/latest/jvm/stdlib/")
        }

        maybeCreate("androidSdk") {
          enabled.convention(this@dss.enableAndroidDocumentationLink)
          url("https://developer.android.com/reference/kotlin/")
        }

        maybeCreate("androidX") {
          enabled.convention(this@dss.enableAndroidDocumentationLink)
          url("https://developer.android.com/reference/kotlin/")
          packageListUrl("https://developer.android.com/reference/kotlin/androidx/package-list")
        }
      }
    }
  }


  private fun initDokkatooTasks(
    target: Project,
    dokkatooExtension: DokkatooExtension,
  ) {
    target.tasks.register<DokkatooTask>(taskNames.generate) {
      description = "Generates Dokkatoo publications for all formats"
      dependsOn(target.tasks.withType<DokkatooGenerateTask>())
    }

    target.tasks.withType<DokkatooGenerateTask>().configureEach {
      cacheDirectory.convention(dokkatooExtension.dokkatooCacheDirectory)
      workerLogFile.convention(temporaryDir.resolve("dokka-worker.log"))
      dokkaConfigurationJsonFile.convention(temporaryDir.resolve("dokka-configuration.json"))

      workerIsolation.convention(dokkatooExtension.dokkaGeneratorIsolation.map { src ->
        when (src) {
          is ClassLoaderIsolation -> src
          is ProcessIsolation     -> {
            // Complicated workaround to copy old properties, to maintain backwards compatibility.
            // Remove when the deprecated task properties are deleted.
            dokkatooExtension.ProcessIsolation {
              @Suppress("DEPRECATION")
              run {
                debug.convention(workerDebugEnabled.orElse(src.debug))
                enableAssertions.convention(src.enableAssertions)
                minHeapSize.convention(workerMinHeapSize.orElse(src.minHeapSize))
                maxHeapSize.convention(workerMaxHeapSize.orElse(src.maxHeapSize))
                jvmArgs.convention(workerJvmArgs.orElse(src.jvmArgs))
                defaultCharacterEncoding.convention(src.defaultCharacterEncoding)
                systemProperties.convention(src.systemProperties)
              }
            }
          }
        }
      })

      publicationEnabled.convention(true)
      onlyIf("publication must be enabled") { publicationEnabled.getOrElse(true) }

      generator.dokkaSourceSets.addAllLater(
        providers.provider {
          // exclude suppressed source sets as early as possible, to avoid unnecessary dependency resolution
          dokkatooExtension.dokkatooSourceSets.filterNot { it.suppress.get() }
        }
      )

      generator.dokkaSourceSets.configureDefaults(
        sourceSetScopeConvention = dokkatooExtension.sourceSetScopeDefault
      )
    }

    target.tasks.withType<DokkatooGenerateModuleTask>().configureEach {
      modulePath.convention(dokkatooExtension.modulePath)
    }
  }


  //region workaround for https://github.com/gradle/gradle/issues/23708
  private fun RegularFileProperty.convention(file: File): RegularFileProperty =
    convention(objects.fileProperty().fileValue(file))

  private fun RegularFileProperty.convention(file: Provider<File>): RegularFileProperty =
    convention(objects.fileProperty().fileProvider(file))
  //endregion


  companion object {
    const val EXTENSION_NAME = "dokkatoo"

    /**
     * The group of all Dokkatoo [Gradle tasks][org.gradle.api.Task].
     *
     * @see org.gradle.api.Task.getGroup
     */
    const val TASK_GROUP = "dokkatoo"

    /** The names of [Gradle tasks][org.gradle.api.Task] created by Dokkatoo */
    val taskNames = TaskNames("")

    /** The names of [Configuration]s created by Dokkatoo */
    @Deprecated("no longer used")
    @Suppress("unused")
    val dependencyContainerNames = DependencyContainerNames("null")

    /** Name of the [Configuration] used to declare dependencies on other subprojects. */
    const val DOKKATOO_CONFIGURATION_NAME = "dokkatoo"

    /** Name of the [Configuration] used to declare dependencies on Dokka Generator plugins. */
    const val DOKKA_GENERATOR_PLUGINS_CONFIGURATION_NAME = "dokkatooPlugin"

    internal val jsonMapper = Json {
      prettyPrint = true
      @OptIn(ExperimentalSerializationApi::class)
      prettyPrintIndent = "  "
    }
  }
}
