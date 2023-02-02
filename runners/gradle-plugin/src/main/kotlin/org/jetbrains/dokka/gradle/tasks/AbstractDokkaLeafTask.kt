package org.jetbrains.dokka.gradle.tasks

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.kotlin.dsl.container
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.dokka.gradle.*

@DisableCachingByDefault(because = "Abstract super-class, not to be instantiated directly")
abstract class AbstractDokkaLeafTask : AbstractDokkaTask() {

    @get:Internal
    val dokkaSourceSets: NamedDomainObjectContainer<GradleDokkaSourceSetBuilder> =
        project.container(GradleDokkaSourceSetBuilder::class, gradleDokkaSourceSetBuilderFactory())

    init {
//        this@AbstractDokkaLeafTask.extensions.add("dokkaSourceSets", dokkaSourceSets)

        project.kotlinOrNull?.sourceSets?.all sourceSet@{
            dokkaSourceSets.register(name) {
                configureWithKotlinSourceSet(this@sourceSet)
            }
        }
    }

    /**
     * Only contains source sets that are marked with `isDocumented`.
     * Non documented source sets are not relevant for Gradle's UP-TO-DATE mechanism, as well
     * as task dependency graph.
     */
    @get:Nested
    protected val unsuppressedSourceSets: List<GradleDokkaSourceSetBuilder>
        get() = dokkaSourceSets
            .toList()
            .also(::checkSourceSetDependencies)
            .filterNot { it.suppress.get() }
}
