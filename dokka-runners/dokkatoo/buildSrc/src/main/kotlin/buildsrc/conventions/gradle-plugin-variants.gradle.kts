package buildsrc.conventions

import org.gradle.api.attributes.plugin.GradlePluginApiVersion.GRADLE_PLUGIN_API_VERSION_ATTRIBUTE

plugins {
  id("buildsrc.conventions.base")
  `java-gradle-plugin`
}

fun registerGradleVariant(name: String, gradleVersion: String) {
  val variantSources = sourceSets.create(name)

  java {
    registerFeature(variantSources.name) {
      usingSourceSet(variantSources)
      capability("${project.group}", "${project.name}", "${project.version}")

      withJavadocJar()
      withSourcesJar()
    }
  }

  configurations
    .matching { it.isCanBeConsumed && it.name.startsWith(variantSources.name) }
    .configureEach {
      attributes {
        attribute(GRADLE_PLUGIN_API_VERSION_ATTRIBUTE, objects.named(gradleVersion))
      }
    }

  tasks.named<Copy>(variantSources.processResourcesTaskName) {
    val copyPluginDescriptors = rootSpec.addChild()
    copyPluginDescriptors.into("META-INF/gradle-plugins")
//        copyPluginDescriptors.into(tasks.pluginDescriptors.flatMap { it.outputDirectory })
    copyPluginDescriptors.from(tasks.pluginDescriptors)
  }

  dependencies {
    add(variantSources.compileOnlyConfigurationName, gradleApi())
  }
}

registerGradleVariant("gradle7", "7.6")
registerGradleVariant("gradle8", "8.0")
