package buildsrc.conventions

import buildsrc.settings.*
import buildsrc.tasks.*
import buildsrc.utils.*

plugins {
  id("buildsrc.conventions.base")
  id("buildsrc.conventions.dokka-source-downloader")
  id("buildsrc.conventions.maven-publish-test")
  id("buildsrc.conventions.dokkatoo-example-projects-base")
}

val mavenPublishTestExtension = extensions.getByType<MavenPublishTestSettings>()
val dokkaTemplateProjectSettings =
  extensions.create<DokkaTemplateProjectSettings>(
    DokkaTemplateProjectSettings.EXTENSION_NAME,
    { project.copySpec() }
  ).apply {
    this.destinationBaseDir.convention(layout.projectDirectory)
  }

val prepareDokkaSource by tasks.existing(Sync::class)

dokkaTemplateProjectSettings.dokkaSourceDir.convention(
  prepareDokkaSource.flatMap {
    layout.dir(providers.provider {
      it.destinationDir
    })
  }
)

tasks.withType<SetupDokkaProjects>().configureEach {
  dependsOn(prepareDokkaSource)

  dokkaSourceDir.convention(dokkaTemplateProjectSettings.dokkaSourceDir)
  destinationBaseDir.convention(dokkaTemplateProjectSettings.destinationBaseDir)

  templateProjects.addAllLater(provider {
    dokkaTemplateProjectSettings.templateProjects
  })
}

val setupDokkaTemplateProjects by tasks.registering(SetupDokkaProjects::class)

fun createDokkatooExampleProjectsSettings(
  projectDir: Directory = project.layout.projectDirectory
): DokkatooExampleProjectsSettings {
  return extensions.create<DokkatooExampleProjectsSettings>(
    DokkatooExampleProjectsSettings.EXTENSION_NAME
  ).apply {

    // find all Gradle settings files
    val settingsFiles = projectDir.asFileTree
      .matching {
        include(
          "**/*dokkatoo*/**/settings.gradle.kts",
          "**/*dokkatoo*/**/settings.gradle",
        )
      }.files

    // for each settings file, create a DokkatooExampleProjectSpec
    settingsFiles.forEach {
      val destinationDir = it.parentFile
      val name = destinationDir.toRelativeString(projectDir.asFile).toAlphaNumericCamelCase()
      exampleProjects.register(name) {
        this.exampleProjectDir.set(destinationDir)
      }
    }

    exampleProjects.configureEach {
      gradlePropertiesContent.add(
        mavenPublishTestExtension.testMavenRepoPath.map { testMavenRepoPath ->
          "testMavenRepo=$testMavenRepoPath"
        }
      )
    }
  }
}

val dokkatooExampleProjectsSettings = createDokkatooExampleProjectsSettings()

val updateDokkatooExamplesGradleProperties by tasks.registering(
  UpdateDokkatooExampleProjects::class
) {
  group = DokkatooExampleProjectsSettings.TASK_GROUP

  mustRunAfter(tasks.withType<SetupDokkaProjects>())

  exampleProjects.addAllLater(providers.provider {
    dokkatooExampleProjectsSettings.exampleProjects
  })
}

val dokkatooVersion = provider { project.version.toString() }

val updateDokkatooExamplesBuildFiles by tasks.registering {
  group = DokkatooExampleProjectsSettings.TASK_GROUP
  description = "Update the Gradle build files in the Dokkatoo examples"

  outputs.upToDateWhen { false }

  mustRunAfter(tasks.withType<SetupDokkaProjects>())
  shouldRunAfter(updateDokkatooExamplesGradleProperties)

  val dokkatooVersion = dokkatooVersion

  val dokkatooDependencyVersionMatcher = """
    \"dev\.adamko\.dokkatoo\:dokkatoo\-plugin\:([^"]+?)\"
    """.trimIndent().toRegex()

  val dokkatooPluginVersionMatcher = """
    id[^"]+?"dev\.adamko\.dokkatoo".+?version "([^"]+?)"
    """.trimIndent().toRegex()

  val gradleBuildFiles =
    layout.projectDirectory.asFileTree
      .matching {
        include(
          "**/*dokkatoo*/**/build.gradle.kts",
          "**/*dokkatoo*/**/build.gradle",
        )
      }.elements
  outputs.files(gradleBuildFiles)

  doLast {
    gradleBuildFiles.get().forEach { fileLocation ->
      val file = fileLocation.asFile
      if (file.exists()) {
        file.writeText(
          file.readText()
            .replace(dokkatooPluginVersionMatcher) {
              val oldVersion = it.groupValues[1]
              it.value.replace(oldVersion, dokkatooVersion.get())
            }
            .replace(dokkatooDependencyVersionMatcher) {
              val oldVersion = it.groupValues[1]
              it.value.replace(oldVersion, dokkatooVersion.get())
            }
        )
      }
    }
  }
}


val updateDokkatooExamples by tasks.registering {
  group = DokkatooExampleProjectsSettings.TASK_GROUP
  description = "lifecycle task for all '${DokkatooExampleProjectsSettings.TASK_GROUP}' tasks"
  dependsOn(
    setupDokkaTemplateProjects,
    updateDokkatooExamplesGradleProperties,
    updateDokkatooExamplesBuildFiles,
  )
}

tasks.assemble {
  dependsOn(updateDokkatooExamples)
  dependsOn(setupDokkaTemplateProjects)
}
