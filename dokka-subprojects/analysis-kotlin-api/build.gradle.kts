/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import dokkabuild.overridePublicationArtifactId
import dokkabuild.utils.downloadLatestKotlinStdlibJvmSources
import dokkabuild.utils.systemProperty
import org.gradle.api.tasks.PathSensitivity.RELATIVE

plugins {
    id("dokkabuild.kotlin-jvm")
    id("dokkabuild.publish-jvm")
    id("dokkabuild.test-k2")
    `java-test-fixtures`
}

overridePublicationArtifactId("analysis-kotlin-api")

dependencies {
    compileOnly(projects.dokkaSubprojects.dokkaCore)

    testFixturesApi(projects.dokkaSubprojects.dokkaCore)

    testImplementation(libs.kotlin.test)

    symbolsTestImplementation(project(path = ":dokka-subprojects:analysis-kotlin-symbols", configuration = "shadow"))
    descriptorsTestImplementation(
        project(path = ":dokka-subprojects:analysis-kotlin-descriptors", configuration = "shadow")
    )
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


//region Download and unpack kotlin-stdlib, so EnumTemplatesTest can test synthetic enum functions.
val kotlinStdlibSourcesDir = downloadLatestKotlinStdlibJvmSources(project)
tasks.withType<Test>().configureEach {
    systemProperty
        .inputDirectory("kotlinStdlibSourcesDir", kotlinStdlibSourcesDir)
        .withPathSensitivity(RELATIVE)
}
//endregion