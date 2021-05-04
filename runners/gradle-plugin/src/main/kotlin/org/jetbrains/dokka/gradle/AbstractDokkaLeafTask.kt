package org.jetbrains.dokka.gradle;

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.tasks.Internal

abstract class AbstractDokkaLeafTask() : AbstractDokkaTask() {

    @get:Internal
    val dokkaSourceSets: NamedDomainObjectContainer<GradleDokkaSourceSetBuilder> =
        project.container(GradleDokkaSourceSetBuilder::class.java, gradleDokkaSourceSetBuilderFactory())
            .also { container ->
                DslObject(this).extensions.add("dokkaSourceSets", container)
                project.kotlinOrNull?.sourceSets?.all { kotlinSourceSet ->
                    container.register(kotlinSourceSet.name) { dokkaSourceSet ->
                        dokkaSourceSet.configureWithKotlinSourceSet(kotlinSourceSet)
                    }
                }
            }
}
