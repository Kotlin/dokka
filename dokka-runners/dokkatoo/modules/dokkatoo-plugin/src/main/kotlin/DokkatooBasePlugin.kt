package org.jetbrains.dokka.dokkatoo

import org.jetbrains.dokka.dokkatoo.distributions.DokkatooConfigurationAttributes
import org.jetbrains.dokka.dokkatoo.distributions.DokkatooConfigurationAttributes.Companion.DOKKATOO_BASE_ATTRIBUTE
import org.jetbrains.dokka.dokkatoo.distributions.DokkatooConfigurationAttributes.Companion.DOKKATOO_CATEGORY_ATTRIBUTE
import org.jetbrains.dokka.dokkatoo.distributions.DokkatooConfigurationAttributes.Companion.DOKKA_FORMAT_ATTRIBUTE
import org.jetbrains.dokka.dokkatoo.dokka.parameters.DokkaSourceSetSpec
import org.jetbrains.dokka.dokkatoo.dokka.parameters.KotlinPlatform
import org.jetbrains.dokka.dokkatoo.dokka.parameters.VisibilityModifier
import org.jetbrains.dokka.dokkatoo.internal.*
import org.jetbrains.dokka.dokkatoo.tasks.DokkatooGenerateTask
import org.jetbrains.dokka.dokkatoo.tasks.DokkatooPrepareModuleDescriptorTask
import org.jetbrains.dokka.dokkatoo.tasks.DokkatooTask
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
import org.gradle.api.tasks.TaskContainer
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

    target.tasks.createDokkaLifecycleTasks()

    val configurationAttributes = objects.newInstance<DokkatooConfigurationAttributes>()

    target.dependencies.attributesSchema {
      attribute(DOKKATOO_BASE_ATTRIBUTE)
      attribute(DOKKATOO_CATEGORY_ATTRIBUTE)
      attribute(DOKKA_FORMAT_ATTRIBUTE)
    }

    target.configurations.register(dependencyContainerNames.dokkatoo) {
      description = "Fetch all Dokkatoo files from all configurations in other subprojects"
      asConsumer()
      isVisible = false
      attributes {
        attribute(DOKKATOO_BASE_ATTRIBUTE, configurationAttributes.dokkatooBaseUsage)
      }
    }

    configureDokkaPublicationsDefaults(dokkatooExtension)
    dokkatooExtension.dokkatooSourceSets.configureDefaults(
      sourceSetScopeConvention = dokkatooExtension.sourceSetScopeDefault
    )

    target.tasks.withType<DokkatooGenerateTask>().configureEach {
      cacheDirectory.convention(dokkatooExtension.dokkatooCacheDirectory)
      workerDebugEnabled.convention(false)
      workerLogFile.convention(temporaryDir.resolve("dokka-worker.log"))
      workerJvmArgs.set(
        listOf(
          //"-XX:MaxMetaspaceSize=512m",
          "-XX:+HeapDumpOnOutOfMemoryError",
          "-XX:+AlwaysPreTouch", // https://github.com/gradle/gradle/issues/3093#issuecomment-387259298
          //"-XX:StartFlightRecording=disk=true,name={path.drop(1).map { if (it.isLetterOrDigit()) it else '-' }.joinToString("")},dumponexit=true,duration=30s",
          //"-XX:FlightRecorderOptions=repository=$baseDir/jfr,stackdepth=512",
        )
      )
      dokkaConfigurationJsonFile.convention(temporaryDir.resolve("dokka-configuration.json"))
    }

    target.tasks.withType<DokkatooPrepareModuleDescriptorTask>().configureEach {
      moduleName.convention(dokkatooExtension.moduleName)
      includes.from(providers.provider { dokkatooExtension.dokkatooSourceSets.flatMap { it.includes } })
      modulePath.convention(dokkatooExtension.modulePath)
    }

    target.tasks.withType<DokkatooGenerateTask>().configureEach {

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

    dokkatooExtension.dokkatooSourceSets.configureDefaults(
      sourceSetScopeConvention = dokkatooExtension.sourceSetScopeDefault
    )
  }

  private fun createExtension(project: Project): DokkatooExtension {
    val dokkatooExtension = project.extensions.create<DokkatooExtension>(EXTENSION_NAME).apply {
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
      dokkatooConfigurationsDirectory.convention(layout.buildDirectory.dir("dokka-config"))
    }

    dokkatooExtension.versions {
      jetbrainsDokka.convention(DokkatooConstants.DOKKA_VERSION)
      jetbrainsMarkdown.convention("0.3.1")
      freemarker.convention("2.3.31")
      kotlinxHtml.convention("0.8.0")
      kotlinxCoroutines.convention("1.6.4")
    }

    return dokkatooExtension
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

            // indeterminate source sets should be named by the Kotlin platform
            else                  -> platform.displayName
          }
        }
      )
      documentedVisibilities.convention(setOf(VisibilityModifier.PUBLIC))
      jdkVersion.convention(8)

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
      // automatically added by DokkatooKotlinAdapter will have a sensible value for suppress.
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

  private fun TaskContainer.createDokkaLifecycleTasks() {
    register<DokkatooTask>(taskNames.generate) {
      description = "Generates Dokkatoo publications for all formats"
      dependsOn(withType<DokkatooGenerateTask>())
    }
  }

  // workaround for https://github.com/gradle/gradle/issues/23708
  private fun RegularFileProperty.convention(file: File): RegularFileProperty =
    convention(objects.fileProperty().fileValue(file))

  // workaround for https://github.com/gradle/gradle/issues/23708
  private fun RegularFileProperty.convention(file: Provider<File>): RegularFileProperty =
    convention(objects.fileProperty().fileProvider(file))

  companion object {

    const val EXTENSION_NAME = "dokkatoo"

    /**
     * The group of all Dokkatoo [Gradle tasks][org.gradle.api.Task].
     *
     * @see org.gradle.api.Task.getGroup
     */
    const val TASK_GROUP = "dokkatoo"

    /** The names of [Gradle tasks][org.gradle.api.Task] created by Dokkatoo */
    val taskNames = TaskNames(null)

    /** The names of [Configuration]s created by Dokkatoo */
    val dependencyContainerNames = DependencyContainerNames(null)

    internal val jsonMapper = Json {
      prettyPrint = true
      @OptIn(ExperimentalSerializationApi::class)
      prettyPrintIndent = "  "
    }
  }

  @DokkatooInternalApi
  abstract class HasFormatName {
    abstract val formatName: String?

    /** Appends [formatName] to the end of the string, camelcase style, if [formatName] is not null */
    protected fun String.appendFormat(): String =
      when (val name = formatName) {
        null -> this
        else -> this + name.uppercaseFirstChar()
      }
  }

  /**
   * Names of the Gradle [Configuration]s used by the [Dokkatoo Plugin][DokkatooBasePlugin].
   *
   * Beware the confusing terminology:
   * - [Gradle Configurations][org.gradle.api.artifacts.Configuration] - share files between subprojects. Each has a name.
   * - [DokkaConfiguration][org.jetbrains.dokka.DokkaConfiguration] - parameters for executing the Dokka Generator
   */
  @DokkatooInternalApi
  class DependencyContainerNames(override val formatName: String?) : HasFormatName() {

    val dokkatoo = "dokkatoo".appendFormat()

    /** Name of the [Configuration] that _consumes_ all [org.jetbrains.dokka.DokkaConfiguration.DokkaModuleDescription] files */
    val dokkatooModuleFilesConsumer = "dokkatooModule".appendFormat()

    /** Name of the [Configuration] that _provides_ all [org.jetbrains.dokka.DokkaConfiguration.DokkaModuleDescription] files to other projects */
    val dokkatooModuleFilesProvider = "dokkatooModuleElements".appendFormat()

    /**
     * Classpath used to execute the Dokka Generator.
     *
     * Extends [dokkaPluginsClasspath], so Dokka plugins and their dependencies are included.
     */
    val dokkaGeneratorClasspath = "dokkatooGeneratorClasspath".appendFormat()

    /** Dokka Plugins (including transitive dependencies, so this can be passed to the Dokka Generator Worker classpath) */
    val dokkaPluginsClasspath = "dokkatooPlugin".appendFormat()

    /**
     * Dokka Plugins (excluding transitive dependencies) will be used to create Dokka Generator Parameters
     *
     * Generally, this configuration should not be invoked manually. Instead, use [dokkaPluginsClasspath].
     */
    val dokkaPluginsIntransitiveClasspath = "dokkatooPluginIntransitive".appendFormat()
  }

  @DokkatooInternalApi
  class TaskNames(override val formatName: String?) : HasFormatName() {
    val generate = "dokkatooGenerate".appendFormat()
    val generatePublication = "dokkatooGeneratePublication".appendFormat()
    val generateModule = "dokkatooGenerateModule".appendFormat()
    val prepareModuleDescriptor = "prepareDokkatooModuleDescriptor".appendFormat()
  }
}
