/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import dokkabuild.tasks.GitCheckoutTask

plugins {
    id("dokkabuild.test-integration")
    id("dokkabuild.setup-maven-cli")
}

dependencies {
    implementation(projects.utilities)

    implementation(kotlin("test-junit5"))
    implementation(libs.junit.jupiterApi)
}

val templateProjectsDir = layout.projectDirectory.dir("projects")

val dokkaSubprojects = gradle.includedBuild("dokka")
val mavenPlugin = gradle.includedBuild("runner-maven-plugin")

tasks.integrationTest {
    dependsOn(
        dokkaSubprojects.task(":publishToMavenLocal"),
        mavenPlugin.task(":publishToMavenLocal"),
        checkoutBioJava,
    )

    dependsOn(tasks.installMavenBinary)
    val mvn = mavenCliSetup.mvn
    inputs.file(mvn)


    doFirst("workaround for https://github.com/gradle/gradle/issues/24267") {
        environment("DOKKA_VERSION", project.version)
        environment("MVN_BINARY_PATH", mvn.get().asFile.invariantSeparatorsPath)
    }
}

val checkoutBioJava by tasks.registering(GitCheckoutTask::class) {
    uri = "https://github.com/biojava/biojava.git"
    commitId = "059fbf1403d0704801df1427b0ec925102a645cd"
    destination = templateProjectsDir.dir("biojava/biojava")
}
