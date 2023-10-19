package buildsrc.tasks

import buildsrc.settings.DokkaTemplateProjectSettings.DokkaTemplateProjectSpec
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.*

abstract class SetupDokkaProjects @Inject constructor(
  private val fs: FileSystemOperations,
  private val layout: ProjectLayout,
  private val providers: ProviderFactory,
) : DefaultTask() {

  @get:OutputDirectories
  val destinationDirs: FileCollection
    get() = layout.files(
      destinationBaseDir.map { base ->
        templateProjects.map { spec -> base.dir(spec.destinationPath) }
      }
    )

  @get:Internal // tracked by destinationDirs
  abstract val destinationBaseDir: DirectoryProperty

  @get:Nested
  abstract val templateProjects: NamedDomainObjectContainer<DokkaTemplateProjectSpec>

  @get:InputDirectory
  abstract val dokkaSourceDir: DirectoryProperty

  @get:InputFiles
  val additionalFiles: FileCollection
    get() = layout.files(
      providers.provider {
        templateProjects.map { it.additionalFiles }
      }
    )

  init {
    group = "dokka examples"
  }

  @TaskAction
  internal fun action() {
    val dokkaSourceDir = dokkaSourceDir.get()
    val destinationBaseDir = destinationBaseDir.get()
    val templateProjects = templateProjects.filter { it.destinationPath.isPresent }

    templateProjects.forEach { spec ->
      fs.sync {
        with(spec.copySpec)

        from(dokkaSourceDir.dir(spec.sourcePath))

        from(
          spec.additionalPaths.get().map { additionalPath ->
            dokkaSourceDir.asFile.resolve(additionalPath)
          }
        )

        from(spec.additionalFiles)

        into(destinationBaseDir.dir(spec.destinationPath))
      }
    }
  }
}
