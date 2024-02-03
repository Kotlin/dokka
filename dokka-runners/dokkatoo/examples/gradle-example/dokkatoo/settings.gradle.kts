rootProject.name = "gradle-example"

pluginManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
    maven(providers.gradleProperty("testMavenRepo"))
  }
}

dependencyResolutionManagement {
  @Suppress("UnstableApiUsage")
  repositories {
    mavenCentral()
    maven(providers.gradleProperty("testMavenRepo"))
  }
}
