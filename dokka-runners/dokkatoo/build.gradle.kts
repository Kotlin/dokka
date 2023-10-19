import buildsrc.utils.excludeGeneratedGradleDsl
import buildsrc.utils.initIdeProjectLogo

plugins {
  buildsrc.conventions.base
  idea
}

group = "org.jetbrains.dokka.dokkatoo"
version = "2.1.0-SNAPSHOT"


idea {
  module {
    excludeGeneratedGradleDsl(layout)

    excludeDirs.apply {
      // exclude .gradle, IDE dirs from nested projects (e.g. example & template projects)
      // so IntelliJ project-wide search isn't cluttered with irrelevant files
      val excludedDirs = setOf(
        ".idea",
        ".gradle",
        "build",
        "gradle/wrapper",
        "ANDROID_SDK",
      )
      addAll(
        projectDir.walk().filter { file ->
          excludedDirs.any {
            file.invariantSeparatorsPath.endsWith(it)
          }
        }
      )
    }
  }
}

initIdeProjectLogo("modules/docs/images/logo-icon.svg")

val dokkatooVersion by tasks.registering {
  description = "prints the Dokkatoo project version (used during release to verify the version)"
  group = "help"
  val version = providers.provider { project.version }
  doLast {
    logger.quiet("${version.orNull}")
  }
}
