/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.it.gradle

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.internal.DefaultGradleRunner
import org.jetbrains.dokka.it.gradle.junit.TestedVersions


/** Edit environment variables in the Gradle Runner */
@Deprecated("Windows does not support withEnvironment - https://github.com/gradle/gradle/issues/23959")
fun GradleRunner.withEnvironment(build: MutableMap<String, String?>.() -> Unit): GradleRunner {
    val env = environment ?: mutableMapOf()
    env.build()
    return withEnvironment(env)
}


inline fun GradleRunner.build(
    handleResult: BuildResult.() -> Unit
): Unit = build().let(handleResult)


inline fun GradleRunner.buildAndFail(
    handleResult: BuildResult.() -> Unit
): Unit = buildAndFail().let(handleResult)


// workaround for https://github.com/gradle/gradle/issues/1043
fun GradleRunner.withJvmArguments(
    jvmArguments: Collection<String>
): GradleRunner = (this as DefaultGradleRunner).withJvmArguments(*jvmArguments.toTypedArray())


/**
 * Helper function to _append_ [arguments] to any existing
 * [GradleRunner arguments][GradleRunner.getArguments].
 */
fun GradleRunner.addArguments(
    vararg arguments: String
): GradleRunner =
    withArguments(this@addArguments.arguments + arguments)


/**
 * Get the name of the task, without the leading [BuildTask.getPath].
 */
val BuildTask.name: String
    get() = path.substringAfterLast(':')

/**
 * `true` if the Gradle version fully supports CC re-use.
 *
 * This is specifically used to test DGP doesn't register unrelated CC inputs.
 * https://github.com/Kotlin/dokka/issues/4467
 *
 * Gradle 9.1 is the first version that properly supported CC re-use,
 * but 9.2 and 9.3 are bugged https://github.com/gradle/gradle/issues/35998.
 * It's fixed in 9.4.
 * Once the Dokka build updates to use Gradle 9.4, this property can be removed.
 */
fun TestedVersions.hasGradleVersionThatSupportsCcReuse(): Boolean =
    gradle.majorAndMinorVersions == "9.1"
            || (gradle.major >= 9 && gradle.minor >= 4)
