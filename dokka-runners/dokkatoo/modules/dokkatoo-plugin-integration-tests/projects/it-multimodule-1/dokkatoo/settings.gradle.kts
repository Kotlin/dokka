rootProject.name = "it-multimodule-1"

pluginManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
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
