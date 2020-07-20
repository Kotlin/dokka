package org.jetbrains.dokka.gradle

import org.gradle.kotlin.dsl.withType
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.dokka.*
import java.net.URL
import kotlin.test.Test
import kotlin.test.assertEquals

class DokkaConfigurationJsonTest {

    @Test
    fun `DokkaTask configuration toJsonString then parseJson`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("org.jetbrains.dokka")
        val dokkaTask = project.tasks.withType<DokkaTask>().first()
        dokkaTask.plugins.withDependencies { dependencies ->
            dependencies.clear()
        }
        dokkaTask.apply {
            this.failOnWarning = true
            this.offlineMode = true
            this.outputDirectory = "customOutputDir"
            this.cacheRoot = "customCacheRoot"
            this.pluginsConfiguration["0"] = "a"
            this.pluginsConfiguration["1"] = "b"
            this.dokkaSourceSets.create("main") { sourceSet ->
                sourceSet.moduleDisplayName = "moduleDisplayName"
                sourceSet.displayName = "customSourceSetDisplayName"
                sourceSet.reportUndocumented = true

                sourceSet.externalDocumentationLink { link ->
                    link.packageListUrl = URL("http://some.url")
                    link.url = URL("http://some.other.url")
                }
                sourceSet.collectKotlinTasks = {
                    println(this@DokkaConfigurationJsonTest)
                    println("This lambda is capturing the entire test")
                    emptyList()
                }

                sourceSet.perPackageOption { packageOption ->
                    packageOption.includeNonPublic = true
                    packageOption.reportUndocumented = true
                    packageOption.skipDeprecated = true
                }
            }
        }

        val sourceConfiguration = dokkaTask.getConfigurationOrThrow()
        val configurationJson = sourceConfiguration.toJsonString()
        val parsedConfiguration = DokkaConfigurationImpl(configurationJson)

        assertEquals(
            DokkaConfigurationImpl(
                failOnWarning = sourceConfiguration.failOnWarning,
                offlineMode = sourceConfiguration.offlineMode,
                outputDir = sourceConfiguration.outputDir,
                cacheRoot = sourceConfiguration.cacheRoot,
                pluginsClasspath = emptyList(),
                pluginsConfiguration = sourceConfiguration.pluginsConfiguration.toMap(),
                sourceSets = listOf(
                    DokkaSourceSetImpl(
                        moduleDisplayName = sourceConfiguration.sourceSets.single().moduleDisplayName,
                        displayName = sourceConfiguration.sourceSets.single().displayName,
                        reportUndocumented = sourceConfiguration.sourceSets.single().reportUndocumented,
                        externalDocumentationLinks = sourceConfiguration.sourceSets.single().externalDocumentationLinks
                            .map { link ->
                                ExternalDocumentationLinkImpl(
                                    url = link.url,
                                    packageListUrl = link.packageListUrl
                                )
                            },
                        perPackageOptions = sourceConfiguration.sourceSets.single().perPackageOptions.map { option ->
                            PackageOptionsImpl(
                                prefix = option.prefix,
                                includeNonPublic = option.includeNonPublic,
                                reportUndocumented = option.reportUndocumented,
                                skipDeprecated = option.skipDeprecated,
                                suppress = option.suppress
                            )
                        },
                        sourceSetID = sourceConfiguration.sourceSets.single().sourceSetID,
                        sourceRoots = sourceConfiguration.sourceSets.single().sourceRoots.map { sourceRoot ->
                            SourceRootImpl(sourceRoot.path)
                        }
                    )
                )
            ), parsedConfiguration
        )
        println(parsedConfiguration)
    }
}
