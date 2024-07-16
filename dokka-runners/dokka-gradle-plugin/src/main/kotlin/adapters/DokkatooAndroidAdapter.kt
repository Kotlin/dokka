package dev.adamko.dokkatoo.adapters

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.TestExtension
import dev.adamko.dokkatoo.DokkatooBasePlugin
import dev.adamko.dokkatoo.DokkatooExtension
import dev.adamko.dokkatoo.dokka.parameters.KotlinPlatform
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import dev.adamko.dokkatoo.internal.PluginId
import dev.adamko.dokkatoo.internal.artifactType
import java.io.File
import javax.inject.Inject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.*

@DokkatooInternalApi
abstract class DokkatooAndroidAdapter @Inject constructor(
  private val objects: ObjectFactory,
) : Plugin<Project> {

  override fun apply(project: Project) {
    logger.info("applied DokkatooAndroidAdapter to ${project.path}")

    project.plugins.withType<DokkatooBasePlugin>().configureEach {
      project.pluginManager.apply {
        withPlugin(PluginId.AndroidBase) { configure(project) }
        withPlugin(PluginId.AndroidApplication) { configure(project) }
        withPlugin(PluginId.AndroidLibrary) { configure(project) }
      }
    }
  }

  protected fun configure(project: Project) {
    val dokkatooExtension = project.extensions.getByType<DokkatooExtension>()

    val androidExt = AndroidExtensionWrapper(project) ?: return

    dokkatooExtension.dokkatooSourceSets.configureEach {

      classpath.from(
        androidExt.bootClasspath()
      )

      classpath.from(
        analysisPlatform.map { analysisPlatform ->
          when (analysisPlatform) {
            KotlinPlatform.AndroidJVM ->
              AndroidClasspathCollector(
                androidExt = androidExt,
                objects = objects,
              )

            else                      ->
              objects.fileCollection()
          }
        }
      )
    }
  }

  @DokkatooInternalApi
  companion object
}


private val logger = Logging.getLogger(DokkatooAndroidAdapter::class.java)


/** Create a [AndroidExtensionWrapper] */
private fun AndroidExtensionWrapper(
  project: Project
): AndroidExtensionWrapper? {
  val androidExt: BaseExtension = try {
    project.extensions.getByType()
  } catch (ex: Exception) {
    logger.warn("DokkatooAndroidAdapter could not get Android Extension for project ${project.path}")
    return null
  }
  return AndroidExtensionWrapper.forBaseExtension(
    androidExt = androidExt,
    providers = project.providers,
    objects = project.objects
  )
}


/**
 * Wrap the Android extension so that Dokkatoo can still access the configuration names without
 * caring about which AGP version is in use.
 */
private interface AndroidExtensionWrapper {

  fun variantsCompileClasspath(): FileCollection

  fun bootClasspath(): Provider<List<File>>

  companion object {

    fun forBaseExtension(
      androidExt: BaseExtension,
      providers: ProviderFactory,
      objects: ObjectFactory,
    ): AndroidExtensionWrapper {
      return object : AndroidExtensionWrapper {

        override fun variantsCompileClasspath(): FileCollection {
          val androidComponentsCompileClasspath = objects.fileCollection()

          val variants = when (androidExt) {
            is LibraryExtension -> androidExt.libraryVariants
            is AppExtension     -> androidExt.applicationVariants
            is TestExtension    -> androidExt.applicationVariants
            else                -> {
              logger.warn("DokkatooAndroidAdapter found unknown Android Extension $androidExt")
              return objects.fileCollection()
            }
          }

          fun Configuration.collect(artifactType: String) {
            val artifactTypeFiles = incoming
              .artifactView {
                attributes {
                  artifactType(artifactType)
                }
                lenient(true)
              }
              .artifacts
              .resolvedArtifacts
              .map { artifacts -> artifacts.map(ResolvedArtifactResult::getFile) }

            androidComponentsCompileClasspath.from(artifactTypeFiles)
          }

          variants.all {
            compileConfiguration.collect("jar")
            //runtimeConfiguration.collect("jar")
          }

          return androidComponentsCompileClasspath
        }

        override fun bootClasspath(): Provider<List<File>> {
          return providers.provider { androidExt.bootClasspath }
        }
      }
    }
  }
}


/**
 * A utility for determining the classpath of an Android compilation.
 *
 * It's important that this class is separate from [DokkatooAndroidAdapter]. It must be separate
 * because it uses Android Gradle Plugin classes (like [BaseExtension]). Were it not separate, and
 * these classes were present in the function signatures of [DokkatooAndroidAdapter], then when
 * Gradle tries to create a decorated instance of [DokkatooAndroidAdapter] it will throw an
 * exception if the project does not have the Android Gradle Plugin applied, because the AGP
 * classes will be missing.
 */
private object AndroidClasspathCollector {

  operator fun invoke(
    androidExt: AndroidExtensionWrapper,
    objects: ObjectFactory,
  ): FileCollection {
    val compilationClasspath = objects.fileCollection()

    compilationClasspath.from({ androidExt.variantsCompileClasspath() })

    return compilationClasspath
  }
}
