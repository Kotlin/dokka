package org.jetbrains.dokka.gradle

import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.jetbrains.dokka.DokkaBootstrap
import org.jetbrains.dokka.DokkaBootstrapImpl
import kotlin.reflect.KClass

// TODO NOW: Test UP-TO-DATE behaviour
abstract class AbstractDokkaParentTask(
    bootstrapClass: KClass<out DokkaBootstrap> = DokkaBootstrapImpl::class
) : AbstractDokkaTask(bootstrapClass) {

    @Input
    open var dokkaTaskNames: Set<String> = setOf()

    @Input
    var subprojectPaths: Set<String> = project.subprojects.map { project -> project.path }.toSet()

    @get:Internal
    val subprojects: List<Project>
        get() = subprojectPaths.map { path -> project.project(path) }.distinct()

    @get:Nested
    internal val dokkaTasks: List<AbstractDokkaTask>
        get() = dokkaTaskNames.flatMap { dokkaTaskName -> findSubprojectDokkaTasks(dokkaTaskName) }


    /**
     * Will remove a single project from participating in this parent task.
     * Note: This will not remove the [project]s subprojects.
     *
     * @see removeAllProjects
     */
    fun removeSubproject(project: Project) {
        subprojectPaths = subprojectPaths - project.path
    }

    /**
     * Will remove the [project] and all its subprojects from participating in this parent task.
     * @see removeSubproject
     */
    fun removeAllProjects(project: Project) {
        project.allprojects.forEach(::removeSubproject)
    }

    /**
     * Includes the [project] to participate in this parent task.
     * Note: This will not include any of the [project]s subprojects.
     * @see addAllProjects
     */
    fun addSubproject(project: Project) {
        subprojectPaths = (subprojectPaths + project.path)
    }

    /**
     * Includes the [project] and all its subprojects to participate in this parent task.
     * @see addSubproject
     */
    fun addAllProjects(project: Project) {
        project.allprojects.forEach(::addSubproject)
    }

    protected fun findSubprojectDokkaTasks(dokkaTaskNames: Set<String>): List<AbstractDokkaTask> {
        return dokkaTaskNames.flatMap { dokkaTaskName -> findSubprojectDokkaTasks(dokkaTaskName) }
    }

    private fun findSubprojectDokkaTasks(dokkaTaskName: String): List<AbstractDokkaTask> {
        return subprojects.mapNotNull { subproject -> subproject.tasks.findByName(dokkaTaskName) as? DokkaTask }
    }
}
