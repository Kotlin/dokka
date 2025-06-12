/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("PackageDirectoryMismatch")

package org.jetbrains.dokka.gradle

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.kotlin.dsl.container
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Abstract super-class, not to be instantiated directly")
@Deprecated(DOKKA_V1_DEPRECATION_MESSAGE)
abstract class AbstractDokkaLeafTask : @Suppress("DEPRECATION") AbstractDokkaTask() {

    @get:Internal
    val dokkaSourceSets: NamedDomainObjectContainer<@Suppress("DEPRECATION") GradleDokkaSourceSetBuilder> =
        project.container(
            @Suppress("DEPRECATION") GradleDokkaSourceSetBuilder::class,
            @Suppress("DEPRECATION") gradleDokkaSourceSetBuilderFactory(),
        ).also { container ->
            DslObject(this).extensions.add("dokkaSourceSets", container)
            project.kotlinOrNull?.sourceSets?.all sourceSet@{
                container.register(name) {
                    @Suppress("DEPRECATION")
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
    protected val unsuppressedSourceSets: List<@Suppress("DEPRECATION") GradleDokkaSourceSetBuilder>
        get() = dokkaSourceSets
            .toList()
            .also(::checkSourceSetDependencies)
            .filterNot { it.suppress.get() }
}
