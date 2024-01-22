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
    /** file-based Maven repositories that contain Dokka dependencies */
    val projectLocalMavenDirs: List<Path> by systemProperty { it.split(":").map(Paths::get) }

    val repos = projectLocalMavenDirs.withIndex().joinToString("\n\n") {(i, path) ->
        /* language=xml */ """
            <pluginRepository>
                <id>dev-repo-$i</id>
                <url>${path.toUri().toASCIIString()}</url>
            </pluginRepository>
        """.trimIndent()
    }

    return """
        <settings>
            <profiles>
                <profile>
                    <id>maven-dev</id>
                    <pluginRepositories>
                        $repos
                    </pluginRepositories>
                </profile>
            </profiles>
            <activeProfiles>
                <activeProfile>maven-dev</activeProfile>
            </activeProfiles>
        </settings>
    """.trimIndent()
}
