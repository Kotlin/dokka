package buildsrc.settings

import buildsrc.utils.adding
import buildsrc.utils.domainObjectContainer
import javax.inject.Inject
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile

/**
 * Settings for the [buildsrc.conventions.Dokkatoo_example_projects_gradle] convention plugin
 */
abstract class DokkatooExampleProjectsSettings @Inject constructor(
  objects: ObjectFactory,
) : ExtensionAware {

  val exampleProjects: NamedDomainObjectContainer<DokkatooExampleProjectSpec> =
    // create an extension so Gradle will generate DSL accessors
    extensions.adding("exampleProjects", objects.domainObjectContainer())

  abstract class DokkatooExampleProjectSpec(
    private val name: String
  ): Named {

    /** The `gradle.properties` file of the example project */
    @get:OutputFile
    val gradlePropertiesFile: Provider<RegularFile>
      get() = exampleProjectDir.file("gradle.properties")

    /** The directory that contains the example project */
    @get:Internal
    abstract val exampleProjectDir: DirectoryProperty

    /**
     * Content to add to the `gradle.properties` file.
     *
     * Elements may span multiple lines.
     *
     * Elements will be sorted before appending to the file (to improve caching & reproducibility).
     */
    @get:Input
    @get:Optional
    abstract val gradlePropertiesContent: ListProperty<String>

    @Input
    override fun getName(): String = name
  }

  companion object {
    const val TASK_GROUP = "dokkatoo examples"
    const val EXTENSION_NAME = "dokkatooExampleProjects"
  }
}
