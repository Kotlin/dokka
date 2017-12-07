package org.jetbrains

import org.gradle.api.Project

class DependenciesVersionGetter {
    static Properties getVersions(Project project, String artifactVersionSelector) {
        def dep = project.dependencies.create(group: 'teamcity', name: 'dependencies', version: artifactVersionSelector, ext: 'properties')
        def file = project.configurations.detachedConfiguration(dep).resolve().first()

        def prop = new Properties()
        prop.load(new FileReader(file))
        return prop
    }
}
