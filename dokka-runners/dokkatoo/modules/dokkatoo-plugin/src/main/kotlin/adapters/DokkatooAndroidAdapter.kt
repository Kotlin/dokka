package org.jetbrains.dokka.dokkatoo.adapters

import com.android.build.api.dsl.CommonExtension
import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.TestExtension
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.dependency.VariantDependencies
import com.android.build.gradle.internal.publishing.AndroidArtifacts.ArtifactType.CLASSES_JAR
import com.android.build.gradle.internal.publishing.AndroidArtifacts.ArtifactType.PROCESSED_JAR
import org.jetbrains.dokka.dokkatoo.DokkatooBasePlugin
import org.jetbrains.dokka.dokkatoo.DokkatooExtension
import org.jetbrains.dokka.dokkatoo.dokka.parameters.KotlinPlatform
import org.jetbrains.dokka.dokkatoo.internal.DokkatooInternalApi
import org.jetbrains.dokka.dokkatoo.internal.collectIncomingFiles
import javax.inject.Inject
import org.gradle.api.DomainObjectSet
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.type.ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
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
        withPlugin("com.android.base") { configure(project) }
        withPlugin("com.android.application") { configure(project) }
        withPlugin("com.android.library") { configure(project) }
      }
    }
  }

  protected fun configure(project: Project) {
    val dokkatooExtension = project.extensions.getByType<DokkatooExtension>()

    val androidExt = AndroidExtensionWrapper(project)

    if (androidExt == null) {
      logger.warn("DokkatooAndroidAdapter could not get Android Extension for project ${project.path}")
      return
    }

    dokkatooExtension.dokkatooSourceSets.configureEach {

      classpath.from(
        analysisPlatform.map { analysisPlatform ->
          when (analysisPlatform) {
            KotlinPlatform.AndroidJVM ->
              AndroidClasspathCollector(
                androidExt = androidExt,
                configurations = project.configurations,
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
  companion object {
    private val logger = Logging.getLogger(DokkatooAndroidAdapter::class.java)
  }
}

private fun AndroidExtensionWrapper(
  project: Project
): AndroidExtensionWrapper? {

// fetching _all_ configuration names is very brute force and should probably be refined to
// only fetch those that match a specific DokkaSourceSetSpec

  return runCatching {
    val androidExt = project.extensions.getByType<BaseExtension>()
    AndroidExtensionWrapper.forBaseExtension(
      androidExt = androidExt,
      providers = project.providers,
      objects = project.objects
    )
  }.recoverCatching {
    val androidExt = project.extensions.getByType(CommonExtension::class)
    AndroidExtensionWrapper.forCommonExtension(androidExt)
  }.getOrNull()
}

/**
 * Android Gradle Plugin is having a refactor. Try to wrap the Android extension so that Dokkatoo
 * can still access the configuration names without caring about which AGP version is in use.
 */
private interface AndroidExtensionWrapper {
  fun variantConfigurationNames(): Set<String>

  companion object {

    @Suppress("DEPRECATION")
    fun forBaseExtension(
      androidExt: BaseExtension,
      providers: ProviderFactory,
      objects: ObjectFactory,
    ): AndroidExtensionWrapper {
      return object : AndroidExtensionWrapper {
        /** Fetch all configuration names used by all variants. */
        override fun variantConfigurationNames(): Set<String> {
          val collector = objects.domainObjectSet(BaseVariant::class)

          val variants: DomainObjectSet<BaseVariant> =
            collector.apply {
              addAllLater(providers.provider {
                when (androidExt) {
                  is LibraryExtension -> androidExt.libraryVariants
                  is AppExtension     -> androidExt.applicationVariants
                  is TestExtension    -> androidExt.applicationVariants
                  else                -> emptyList()
                }
              })
            }

          return buildSet {
            variants.forEach {
              add(it.compileConfiguration.name)
              add(it.runtimeConfiguration.name)
              add(it.annotationProcessorConfiguration.name)
            }
          }
        }
      }
    }

    fun forCommonExtension(
      androidExt: CommonExtension<*, *, *, *>
    ): AndroidExtensionWrapper {
      return object : AndroidExtensionWrapper {
        /** Fetch all configuration names used by all variants. */
        override fun variantConfigurationNames(): Set<String> {
          return buildSet {
            @Suppress("UnstableApiUsage")
            androidExt.sourceSets.forEach {
              add(it.apiConfigurationName)
              add(it.compileOnlyConfigurationName)
              add(it.implementationConfigurationName)
              add(it.runtimeOnlyConfigurationName)
              add(it.wearAppConfigurationName)
              add(it.annotationProcessorConfigurationName)
            }
          }
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
 * Gradle tries to create a decorated instance of [DokkatooAndroidAdapter] it will if the project
 * does not have the Android Gradle Plugin applied, because the classes will be missing.
 */
private object AndroidClasspathCollector {

  operator fun invoke(
    androidExt: AndroidExtensionWrapper,
    configurations: ConfigurationContainer,
    objects: ObjectFactory,
  ): FileCollection {
    val compilationClasspath = objects.fileCollection()

    fun collectConfiguration(named: String) {
      listOf(
        // need to fetch multiple different types of files, because AGP is weird and doesn't seem
        // to have a 'just give me normal JVM classes' option
        ARTIFACT_TYPE_ATTRIBUTE to PROCESSED_JAR.type,
        ARTIFACT_TYPE_ATTRIBUTE to CLASSES_JAR.type,
      ).forEach { (attribute, attributeValue) ->
        configurations.collectIncomingFiles(named, collector = compilationClasspath) {
          attributes {
            attribute(attribute, attributeValue)
          }
          lenient(true)
        }
      }
    }

    // fetch android.jar
    collectConfiguration(named = VariantDependencies.CONFIG_NAME_ANDROID_APIS)

    val variantConfigurations = androidExt.variantConfigurationNames()

    for (variantConfig in variantConfigurations) {
      collectConfiguration(named = variantConfig)
    }

    return compilationClasspath
  }
}
