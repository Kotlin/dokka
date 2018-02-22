package org.jetbrains

import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.SelfResolvingDependency
import org.gradle.api.publish.internal.ProjectDependencyPublicationResolver
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPublication

static void configure(MavenPublication publication, Project project) {
    publication.artifact(project.tasks.shadowJar)

    publication.pom { MavenPom pom ->
        pom.withXml { xml ->
            def dependenciesNode = xml.asNode().appendNode('dependencies')

            project.configurations.shadow.allDependencies.each {
                //if (! (it instanceof SelfResolvingDependency)) {
                if (it instanceof ProjectDependency) {
                    def projectDependencyResolver = project.gradle.services.get(ProjectDependencyPublicationResolver)
                    ModuleVersionIdentifier identifier = projectDependencyResolver.resolve(it)
                    addDependency(dependenciesNode, identifier)
                } else if (!(it instanceof SelfResolvingDependency)) {
                    addDependency(dependenciesNode, it)
                }

            }
        }
    }
}

private static void addDependency(Node dependenciesNode, dep) {
    def dependencyNode = dependenciesNode.appendNode('dependency')
    dependencyNode.appendNode('groupId', dep.group)
    dependencyNode.appendNode('artifactId', dep.name)
    dependencyNode.appendNode('version', dep.version)
    dependencyNode.appendNode('scope', 'runtime')
}