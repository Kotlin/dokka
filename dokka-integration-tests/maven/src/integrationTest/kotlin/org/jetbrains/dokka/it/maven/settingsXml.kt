/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it.maven

import org.intellij.lang.annotations.Language
import org.jetbrains.dokka.it.systemProperty
import java.nio.file.Path
import java.nio.file.Paths


/** Create `settings.xml` file contents, with the custom dev Maven repos. */
@Language("xml")
fun createSettingsXml(): String {
    /** file-based Maven repositories with Dokka dependencies */
    val devMavenRepositories: List<Path> by systemProperty { repos ->
        repos.split(",").map { Paths.get(it) }
    }

    val pluginRepos = devMavenRepositories
        .withIndex()
        .joinToString("\n") { (i, repoPath) ->
            /* language=xml */
            """
                |<pluginRepository>
                |    <id>devMavenRepo${i}</id>
                |    <url>${repoPath.toUri().toASCIIString()}</url>
                |</pluginRepository>
            """.trimMargin()
        }

    return """
        |<settings>
        |    <profiles>
        |        <profile>
        |            <id>maven-dev</id>
        |            <pluginRepositories>
        |${pluginRepos.prependIndent("                ")}
        |            </pluginRepositories>
        |        </profile>
        |    </profiles>
        |    <activeProfiles>
        |        <activeProfile>maven-dev</activeProfile>
        |    </activeProfiles>
        |</settings>
        |
    """.trimMargin()
}
