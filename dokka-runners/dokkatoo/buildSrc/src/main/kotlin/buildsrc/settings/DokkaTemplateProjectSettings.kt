package buildsrc.settings

import buildsrc.utils.adding
import buildsrc.utils.domainObjectContainer
import buildsrc.utils.toAlphaNumericCamelCase
import javax.inject.Inject
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.kotlin.dsl.*

private typealias TemplateProjectsContainer = NamedDomainObjectContainer<DokkaTemplateProjectSettings.DokkaTemplateProjectSpec>

abstract class DokkaTemplateProjectSettings @Inject constructor(
  private val objects: ObjectFactory,
  private val copySpecs: () -> CopySpec
) : ExtensionAware {

  /** Directory that will contain the projects downloaded from the Dokka source code. */
  abstract val dokkaSourceDir: DirectoryProperty

  abstract val destinationBaseDir: DirectoryProperty

  internal val templateProjects: TemplateProjectsContainer =
    // create an extension so Gradle will generate DSL accessors
    extensions.adding("templateProjects", objects.domainObjectContainer { name ->
      objects.newInstance<DokkaTemplateProjectSpec>(name, copySpecs())
    })

  /**
   * Copy a directory from the Dokka source project into a local directory.
   *
   * @param[source] Source dir, relative to [templateProjectsDir]
   * @param[destination] Destination dir, relative to [destinationBaseDir]
   */
  fun register(
    source: String,
    destination: String,
    configure: DokkaTemplateProjectSpec.() -> Unit = {},
  ) {
    val name = source.toAlphaNumericCamelCase()
    templateProjects.register(name) {
      this.sourcePath.set(source)
      this.destinationPath.set(destination)
      configure()
    }
  }

  fun configureEach(configure: DokkaTemplateProjectSpec.() -> Unit) {
    templateProjects.configureEach(configure)
  }

  /**
   * Details for how to copy a Dokka template project from the Dokka project to a local directory.
   */
  abstract class DokkaTemplateProjectSpec @Inject constructor(
    private val named: String,
    @get:Internal
    internal val copySpec: CopySpec,
  ) : Named {

    @get:Input
    abstract val sourcePath: Property<String>

    @get:Input
    @get:Optional
    abstract val destinationPath: Property<String>

    @get:Input
    abstract val additionalPaths: SetProperty<String>

    @get:InputFiles
    abstract val additionalFiles: ConfigurableFileCollection

    fun configureCopy(configure: CopySpec.() -> Unit) {
      copySpec.configure()
    }

    @Input
    override fun getName(): String = named
  }

  companion object {
    const val EXTENSION_NAME = "dokkaTemplateProjects"
  }
}
