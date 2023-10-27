/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
    id("org.jetbrains.conventions.maven-publish")
    `java-test-fixtures`
}

dependencies {
    compileOnly(projects.core)

    testFixturesApi(projects.core)

    testImplementation(kotlin("test"))
    testImplementation(projects.subprojects.analysisKotlinDescriptors)
}

disableTestFixturesPublishing()

/**
 * Test fixtures are automatically published by default, which at this moment in time is unwanted
 * as the test api is unstable and is internal to the Dokka project, so it shouldn't be used outside of it.
 *
 * @see https://docs.gradle.org/current/userguide/java_testing.html#ex-disable-publishing-of-test-fixtures-variants
 */
fun disableTestFixturesPublishing() {
    val javaComponent = components["java"] as AdhocComponentWithVariants
    javaComponent.withVariantsFromConfiguration(configurations["testFixturesApiElements"]) { skip() }
    javaComponent.withVariantsFromConfiguration(configurations["testFixturesRuntimeElements"]) { skip() }
}

registerDokkaArtifactPublication("analysisKotlinApi") {
    artifactId = "analysis-kotlin-api"
}
