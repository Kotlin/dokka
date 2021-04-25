package org.jetbrains.dokka.gradle;

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.tasks.Internal

abstract class AbstractDokkaLeafTask() : AbstractDokkaTask() {

    @get:Internal
    abstract val dokkaSourceSets: NamedDomainObjectContainer<GradleDokkaSourceSetBuilder>
}
