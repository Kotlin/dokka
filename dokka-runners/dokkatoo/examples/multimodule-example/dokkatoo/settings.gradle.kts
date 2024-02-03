rootProject.name = "dokkatoo-multimodule-example"

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

include(":parentProject")
include(":parentProject:childProjectA")
include(":parentProject:childProjectB")
