/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:Suppress("UnstableApiUsage")

import dokkabuild.utils.systemProperty
import org.gradle.api.attributes.Bundling.BUNDLING_ATTRIBUTE
import org.gradle.api.attributes.Bundling.SHADOWED

plugins {
    id("dokkabuild.kotlin-jvm")
    id("dokkabuild.test-integration")
    id("dokkabuild.test-cli-dependencies")
}

dependencies {
    api(libs.kotlin.test.junit5)
    api(libs.junit.jupiterApi)
    api(projects.utilities)

    dokkaCli("org.jetbrains.dokka:runner-cli")

    //region required dependencies of plugin-base
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
    //endregion
}

testing {
    suites {
        register<JvmTestSuite>("cliIntegrationTest") {
            targets.configureEach {
                testTask.configure {
                    systemProperty
                        .inputFiles("dokkaCliJarPath", configurations.dokkaCliResolver)
                        .withNormalizer(ClasspathNormalizer::class)

                    systemProperty
                        .inputFiles("dokkaPluginsClasspath", configurations.dokkaPluginsClasspathResolver)
                        .withNormalizer(ClasspathNormalizer::class)
                }
            }
        }
    }
}
