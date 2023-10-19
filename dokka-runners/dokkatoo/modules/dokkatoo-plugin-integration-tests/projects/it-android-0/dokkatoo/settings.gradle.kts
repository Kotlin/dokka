rootProject.name = "it-android-0"

pluginManagement {
  repositories {
    maven(providers.gradleProperty("testMavenRepo"))
    mavenCentral()
    google()
    gradlePluginPortal()
  }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
  repositories {
    maven(providers.gradleProperty("testMavenRepo"))
    mavenCentral()
    google()
  }
}
