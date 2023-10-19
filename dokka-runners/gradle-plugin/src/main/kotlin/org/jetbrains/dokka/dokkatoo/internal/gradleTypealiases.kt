/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.dokkatoo.internal

import org.gradle.api.ExtensiblePolymorphicDomainObjectContainer
import org.jetbrains.dokka.dokkatoo.dokka.plugins.DokkaPluginParametersBaseSpec

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
