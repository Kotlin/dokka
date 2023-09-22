package buildsrc.utils

import org.gradle.api.Project
import org.gradle.api.file.ProjectLayout
import org.gradle.plugins.ide.idea.model.IdeaModule


/** exclude generated Gradle code, so it doesn't clog up search results */
fun IdeaModule.excludeGeneratedGradleDsl(layout: ProjectLayout) {

  val generatedSrcDirs = listOf(
    "kotlin-dsl-accessors",
    "kotlin-dsl-external-plugin-spec-builders",
    "kotlin-dsl-plugins",
  )

  excludeDirs.addAll(
    layout.projectDirectory.asFile.walk()
      .filter { it.isDirectory && it.parentFile.name in generatedSrcDirs }
      .flatMap { file ->
        file.walk().maxDepth(1).filter { it.isDirectory }.toList()
      }
  )
}


/** Sets a logo for project IDEs */
fun Project.initIdeProjectLogo(
  svgLogoPath: String
) {
  val logoSvg = rootProject.layout.projectDirectory.file(svgLogoPath)
  val ideaDir = rootProject.layout.projectDirectory.dir(".idea")

  if (
    logoSvg.asFile.exists()
    && ideaDir.asFile.exists()
    && !ideaDir.file("icon.png").asFile.exists()
    && !ideaDir.file("icon.svg").asFile.exists()
  ) {
    copy {
      from(logoSvg) { rename { "icon.svg" } }
      into(ideaDir)
    }
  }
}
