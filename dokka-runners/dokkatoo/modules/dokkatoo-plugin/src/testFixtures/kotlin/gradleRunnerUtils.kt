package org.jetbrains.dokka.dokkatoo.utils

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.internal.DefaultGradleRunner


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


fun GradleRunner.withJvmArguments(
  vararg jvmArguments: String
): GradleRunner = (this as DefaultGradleRunner).withJvmArguments(*jvmArguments)


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
