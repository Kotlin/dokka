plugins {
    `kotlin-dsl`
}

/**
 * These plugins are added to the buildscript classpath, so subprojects can refer to the plugin in the plugins block
 * by using the [PluginDependenciesSpec.id] function, without the need to also call [PluginDependencySpec.version].
 */
val pluginVersions = setOf(
  "com.jfrog.bintray.gradle:gradle-bintray-plugin:1.8.5",
  "com.github.jengelman.gradle.plugins:shadow:5.2.0",
  "com.gradle.publish:plugin-publish-plugin:0.12.0"
).map {
    dependencies.implementation(it) ?: throw NullPointerException("Plugin dependency must not be null!")
}

repositories {
    exclusiveContent {
        forRepository {
            gradlePluginPortal()
        }
        filter {
            pluginVersions.forEach { includeVersion(it.group!!, it.name, it.version!!) }
        }
    }
    jcenter()
    maven("https://dl.bintray.com/kotlin/kotlin-eap/") {
        content {
            includeGroupByRegex("^org\\.jetbrains\\.(anko|dokka|kotlin|kotlinx)(\\..+)?$")
        }
    }
    maven("https://dl.bintray.com/kotlin/kotlin-dev/") {
        content {
            includeGroupByRegex("^org\\.jetbrains\\.(anko|dokka|intellij|kotlin|kotlinx)(\\..+)?$")
        }
    }
}
