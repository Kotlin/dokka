package org.jetbrains.dokka.dokkatoo.internal

import org.jetbrains.dokka.dokkatoo.dokka.plugins.DokkaPluginParametersBaseSpec
import org.gradle.api.ExtensiblePolymorphicDomainObjectContainer

/** Container for all [Dokka Plugin parameters][DokkaPluginParametersBaseSpec]. */
typealias DokkaPluginParametersContainer =
    ExtensiblePolymorphicDomainObjectContainer<DokkaPluginParametersBaseSpec>


/**
 * The path of a Gradle [Project][org.gradle.api.Project]. This is unique per subproject.
 * This is _not_ the file path, which
 * [can be configured to be different to the project path](https://docs.gradle.org/current/userguide/fine_tuning_project_layout.html#sub:modifying_element_of_the_project_tree).
 *
 * Example: `:modules:tests:alpha-project`.
 *
 * @see org.gradle.api.Project.getPath
 */
internal typealias GradleProjectPath = org.gradle.util.Path
