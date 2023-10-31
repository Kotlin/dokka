/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import dokkabuild.overridePublicationArtifactId

plugins {
    id("dokkabuild.kotlin-jvm")
    id("dokkabuild.publish-jvm")
    `java-test-fixtures`
}

overridePublicationArtifactId("analysis-kotlin-api")

dependencies {
    compileOnly(projects.dokkaSubprojects.dokkaCore)

    testFixturesApi(projects.dokkaSubprojects.dokkaCore)

    testImplementation(kotlin("test"))
    testImplementation(projects.dokkaSubprojects.analysisKotlinDescriptors)
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
