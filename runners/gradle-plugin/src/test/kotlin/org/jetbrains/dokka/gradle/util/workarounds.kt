package org.jetbrains.dokka.gradle.util

import org.gradle.api.*
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencySet
import org.jetbrains.dokka.gradle.GradleDokkaSourceSetBuilder
import org.jetbrains.dokka.gradle.GradleExternalDocumentationLinkBuilder
import org.jetbrains.dokka.gradle.GradlePackageOptionsBuilder
import org.jetbrains.dokka.gradle.GradleSourceLinkBuilder


/**
 * Workarounds because `SamWithReceiver` not working in test sources
 * https://youtrack.jetbrains.com/issue/KTIJ-14684
 *
 * The `SamWithReceiver` plugin is automatically applied by the `kotlin-dsl` plugin.
 * It converts all [org.gradle.api.Action] so the parameter is the receiver:
 *
 * ```
 * // with SamWithReceiver ✅
 * tasks.configureEach {
 *   val task: Task = this
 * }
 *
 * // without SamWithReceiver
 * tasks.configureEach { it ->
 *   val task: Task = it
 * }
 * ```
 *
 * This is nice because it means that the Dokka Gradle Plugin more closely matches `build.gradle.kts` files.
 *
 * However, [IntelliJ is bugged](https://youtrack.jetbrains.com/issue/KTIJ-14684) and doesn't
 * acknowledge that `SamWithReceiver` has been applied in test sources. The code works and compiles,
 * but IntelliJ shows red errors.
 *
 * These functions are workarounds, and should be removed ASAP.
 */
@Suppress("unused")
private object Explain

internal fun Project.subprojects_(configure: Project.() -> Unit) =
    subprojects(configure)

@Suppress("SpellCheckingInspection")
internal fun Project.allprojects_(configure: Project.() -> Unit) =
    allprojects(configure)

internal fun <T> DomainObjectCollection<T>.configureEach_(configure: T.() -> Unit) =
    configureEach(configure)

internal fun <T> DomainObjectCollection<T>.all_(configure: T.() -> Unit) =
    all(configure)

internal fun Configuration.withDependencies_(action: DependencySet.() -> Unit): Configuration =
    withDependencies(action)


internal fun <T> NamedDomainObjectContainer<T>.create_(name: String, configure: T.() -> Unit): T =
    create(name, configure)

internal fun <T> NamedDomainObjectContainer<T>.register_(
    name: String,
    configure: T.() -> Unit
): NamedDomainObjectProvider<T> =
    register(name, configure)


internal fun GradleDokkaSourceSetBuilder.externalDocumentationLink_(
    action: GradleExternalDocumentationLinkBuilder.() -> Unit
) = externalDocumentationLink(action)

internal fun GradleDokkaSourceSetBuilder.sourceLink_(
    action: GradleSourceLinkBuilder.() -> Unit
) = sourceLink(action)

internal fun GradleDokkaSourceSetBuilder.perPackageOption_(
    action: GradlePackageOptionsBuilder.() -> Unit
) = perPackageOption(action)
