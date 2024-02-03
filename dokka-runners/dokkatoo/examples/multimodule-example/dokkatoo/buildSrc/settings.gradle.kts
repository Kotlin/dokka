rootProject.name = "buildSrc"

pluginManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
    maven(providers.gradleProperty("testMavenRepo"))
  }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {

  repositoriesMode = RepositoriesMode.PREFER_SETTINGS

  repositories {
    mavenCentral()
    gradlePluginPortal()
    maven(providers.gradleProperty("testMavenRepo"))
  }
}
