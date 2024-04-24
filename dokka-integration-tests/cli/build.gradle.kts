/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:Suppress("UnstableApiUsage")

import org.gradle.api.attributes.Bundling.BUNDLING_ATTRIBUTE
import org.gradle.api.attributes.Bundling.SHADOWED

plugins {
    id("dokkabuild.kotlin-jvm")
    id("dokkabuild.test-cli-dependencies")
    `jvm-test-suite`
}

dependencies {
    api(kotlin("test-junit5"))
    api(libs.junit.jupiterApi)
    api(projects.utilities)

    dokkaCli("org.jetbrains.dokka:runner-cli")

    dokkaPluginsClasspath("org.jetbrains.dokka:plugin-base")
    dokkaPluginsClasspath(libs.kotlinx.html)
    dokkaPluginsClasspath(libs.freemarker)

    val analysisDependency = dokkaBuild.integrationTestUseK2.map { useK2 ->
        if (useK2) {
            "org.jetbrains.dokka:analysis-kotlin-symbols"
        } else {
            "org.jetbrains.dokka:analysis-kotlin-descriptors"
        }
    }
    dokkaPluginsClasspath(analysisDependency) {
        attributes {
            attribute(BUNDLING_ATTRIBUTE, project.objects.named(SHADOWED))
        }
    }
}

/**
 * Provide files required for running Dokka CLI in a build cache friendly way.
 */
abstract class DokkaCliClasspathProvider : CommandLineArgumentProvider {
    @get:Classpath
    abstract val dokkaCli: ConfigurableFileCollection

    @get:Classpath
    abstract val dokkaPluginsClasspath: ConfigurableFileCollection

    override fun asArguments(): Iterable<String> = buildList {
        require(dokkaCli.count() == 1) {
            "Expected a single Dokka CLI JAR, but got ${dokkaCli.count()}"
        }
        add("-D" + "dokkaCliJarPath=" + dokkaCli.singleFile.absolutePath)
        add("-D" + "dokkaPluginsClasspath=" + dokkaPluginsClasspath.joinToString(";") { it.absolutePath })
    }
}


testing {
    suites {
        withType<JvmTestSuite>().configureEach {
            useJUnitJupiter()
        }

        register<JvmTestSuite>("integrationTest") {
            dependencies {
                implementation(project())
            }

            targets.configureEach {
                testTask.configure {
                    jvmArgumentProviders.add(
                        objects.newInstance<DokkaCliClasspathProvider>().apply {
                            dokkaCli.from(configurations.dokkaCliResolver)
                            dokkaPluginsClasspath.from(configurations.dokkaPluginsClasspathResolver)
                        }
                    )
                }
            }
        }
    }
}

tasks.check {
    dependsOn(testing.suites)
}
