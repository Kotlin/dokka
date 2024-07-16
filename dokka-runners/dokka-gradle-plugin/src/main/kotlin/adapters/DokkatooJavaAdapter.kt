package dev.adamko.dokkatoo.adapters

import dev.adamko.dokkatoo.DokkatooExtension
import dev.adamko.dokkatoo.dokka.parameters.DokkaSourceSetSpec
import dev.adamko.dokkatoo.dokka.parameters.KotlinPlatform
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import dev.adamko.dokkatoo.internal.PluginId
import dev.adamko.dokkatoo.internal.or
import dev.adamko.dokkatoo.internal.uppercaseFirstChar
import javax.inject.Inject
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME
import org.gradle.api.tasks.SourceSet.TEST_SOURCE_SET_NAME
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation

/**
 * Apply Java specific configuration to the Dokkatoo plugin.
 *
 * **Must be applied *after* [dev.adamko.dokkatoo.DokkatooBasePlugin]**
 */
@DokkatooInternalApi
abstract class DokkatooJavaAdapter @Inject constructor(
  private val providers: ProviderFactory,
) : Plugin<Project> {

  private val logger = Logging.getLogger(this::class.java)

  override fun apply(project: Project) {
    logger.info("applied DokkatooJavaAdapter to ${project.path}")

    val dokkatoo = project.extensions.getByType<DokkatooExtension>()

    // wait for the Java plugin to be applied
    project.plugins.withType<JavaBasePlugin>().configureEach {
      val java = project.extensions.getByType<JavaPluginExtension>()
      val sourceSets = project.extensions.getByType<SourceSetContainer>()

      detectJavaToolchainVersion(dokkatoo, java)

      val isConflictingPluginPresent = isConflictingPluginPresent(project)
      registerDokkatooSourceSets(dokkatoo, sourceSets, isConflictingPluginPresent)
    }
  }

  /** fetch the  toolchain, and use the language version as Dokka's jdkVersion */
  private fun detectJavaToolchainVersion(
    dokkatoo: DokkatooExtension,
    java: JavaPluginExtension,
  ) {
    // fetch the toolchain, and use the language version as Dokka's jdkVersion
    val toolchainLanguageVersion = java
      .toolchain
      .languageVersion

    dokkatoo.dokkatooSourceSets.configureEach {
      jdkVersion.set(toolchainLanguageVersion.map { it.asInt() }.orElse(11))
    }
  }

  private fun registerDokkatooSourceSets(
    dokkatoo: DokkatooExtension,
    sourceSets: SourceSetContainer,
    isConflictingPluginPresent: Provider<Boolean>,
  ) {
    sourceSets.all jss@{
      register(
        dokkaSourceSets = dokkatoo.dokkatooSourceSets,
        src = this@jss,
        isConflictingPluginPresent = isConflictingPluginPresent,
      )
    }
  }

  /** Register a single [DokkaSourceSetSpec] for [src] */
  private fun register(
    dokkaSourceSets: NamedDomainObjectContainer<DokkaSourceSetSpec>,
    src: SourceSet,
    isConflictingPluginPresent: Provider<Boolean>,
  ) {
    dokkaSourceSets.register(
      "java${src.name.uppercaseFirstChar()}"
    ) {
      suppress.convention(!src.isPublished() or isConflictingPluginPresent)
      sourceRoots.from(src.java)
      analysisPlatform.convention(KotlinPlatform.JVM)

      classpath.from(providers.provider { src.compileClasspath })
      classpath.builtBy(src.compileJavaTaskName)
    }
  }

  /**
   * The Android and Kotlin plugins _also_ add the Java plugin.
   *
   * To prevent generating documentation for the same sources twice, automatically suppress any
   * [DokkaSourceSetSpec] when any Android or Kotlin plugin is present
   *
   * Projects with Android or Kotlin projects present will be handled by [DokkatooAndroidAdapter]
   * or [DokkatooKotlinAdapter].
   */
  private fun isConflictingPluginPresent(
    project: Project
  ): Provider<Boolean> {

    val projectHasKotlinPlugin = providers.provider {
      project.pluginManager.hasPlugin(PluginId.KotlinAndroid)
          || project.pluginManager.hasPlugin(PluginId.KotlinJs)
          || project.pluginManager.hasPlugin(PluginId.KotlinJvm)
          || project.pluginManager.hasPlugin(PluginId.KotlinMultiplatform)
    }

    val projectHasAndroidPlugin = providers.provider {
      project.pluginManager.hasPlugin(PluginId.AndroidBase)
          || project.pluginManager.hasPlugin(PluginId.AndroidApplication)
          || project.pluginManager.hasPlugin(PluginId.AndroidLibrary)
    }

    return projectHasKotlinPlugin or projectHasAndroidPlugin
  }

  @DokkatooInternalApi
  companion object {
    /**
     * Determine if a [KotlinCompilation] is 'publishable', and so should be enabled by default
     * when creating a Dokka publication.
     *
     * Typically, 'main' compilations are publishable and 'test' compilations should be suppressed.
     * This can be overridden manually, though.
     *
     * @see DokkaSourceSetSpec.suppress
     */
    fun SourceSet.isPublished(): Boolean =
      name != TEST_SOURCE_SET_NAME
          && name.startsWith(MAIN_SOURCE_SET_NAME)
  }
}
