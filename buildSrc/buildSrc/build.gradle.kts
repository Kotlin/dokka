plugins {
  `kotlin-dsl`
}
repositories.jcenter()
sourceSets.main {
  /**
   * The classes that are included here are compiled a second time,
   * so they are available on the classpath of `${rootProject.projectDir}/buildSrc/build.gradle.kts`.
   */
  java {
    setSrcDirs(setOf(projectDir.parentFile.resolve("src/main/kotlin")))
    exclude("org/jetbrains/publication.kt")
    exclude("org/jetbrains/ValidatePublications.kt")
  }
}
