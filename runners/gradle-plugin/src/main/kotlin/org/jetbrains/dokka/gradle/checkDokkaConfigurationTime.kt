package org.jetbrains.dokka.gradle

import org.gradle.api.Project
import org.jetbrains.dokka.DokkaException

internal fun checkDokkaConfigurationTime(project: Project) {
    if (project.state.executed) return
    val message = "Project ${project.name}: dokka should not be configured during build script evaluation\n" +
            "Make sure to configure dokka tasks lazily!\n" +
            "dokkaHtml { //... } -> tasks.named(\"dokkaHtml\") { //... }\n" +
            "tasks.withType<DokkaTask>() { //... } -> tasks.withType<DokkaTask>().configureEach { //... }\n" +
            "dokkaSourceSets.all { //... } -> dokkaSourceSets.configureEach { //... }\n" +
            "dokkaSourceSets.getByName(\"main\") { //... } -> dokkaSourceSet.named(\"main\") { //... }\n" +
            "...\n" +
            "See: https://docs.gradle.org/current/userguide/task_configuration_avoidance.html"
    if (project.isAndroidProject()) {
        throw DokkaException(message)
    } else {
        project.logger.warn(message)
    }
}
