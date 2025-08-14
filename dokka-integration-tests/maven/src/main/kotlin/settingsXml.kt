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

    val mavenCentralRepo = Repository(
        id = "MavenCentral-JBCached",
        url = "https://cache-redirector.jetbrains.com/maven-central",
    )

    val pluginRepos = buildList {
        addAll(
            devMavenRepositories.mapIndexed { i, repoPath ->
                Repository(
                    id = "devMavenRepo${i}",
                    url = repoPath.toUri().toASCIIString()
                )
            }
        )
        add(mavenCentralRepo)
    }.joinToString("\n") { it.pluginRepository() }

    return """
        |<settings>
        |    <profiles>
        |        <profile>
        |            <id>maven-dev</id>
        |            <pluginRepositories>
        |${pluginRepos.prependIndent("                ")}
        |            </pluginRepositories>
        |            <repositories>
        |${mavenCentralRepo.repository().prependIndent("            ")}
        |            </repositories>
        |        </profile>
        |    </profiles>
        |    <mirrors>
        |${mavenCentralRepo.mirror(of = "central").prependIndent("        ")}
        |    </mirrors>
        |    <activeProfiles>
        |        <activeProfile>maven-dev</activeProfile>
        |    </activeProfiles>
        |</settings>
        |
    """.trimMargin()
}

private data class Repository(
    val id: String,
    @param:Language("http-url-reference")
    val url: String,
) {
    @Language("XML")
    fun repository() = """
        <repository>
            <id>${id}</id>
            <url>${url}</url>
        </repository>
    """.trimIndent()

    @Language("XML")
    fun pluginRepository() = """
        <pluginRepository>
            <id>${id}</id>
            <url>${url}</url>
        </pluginRepository>
    """.trimIndent()

    @Language("XML")
    fun mirror(of: String) = """
        <mirror>
            <id>${id}</id>
            <url>${url}</url>
            <mirrorOf>${of}</mirrorOf>
        </mirror>
    """.trimIndent()
}
