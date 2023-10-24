rootProject.name = "it-multimodule-1"

pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
    maven(providers.gradleProperty("testMavenRepo"))
  }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
  repositories {
    mavenCentral()
    maven(providers.gradleProperty("testMavenRepo"))
  }
}
