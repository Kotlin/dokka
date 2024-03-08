/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import dokkabuild.tasks.GitCheckoutTask
import org.gradle.api.tasks.PathSensitivity.RELATIVE

plugins {
    id("dokkabuild.test-integration")
    id("dokkabuild.setup-maven-cli")
    id("dokkabuild.dev-maven-publish")
}

dependencies {
    implementation(projects.utilities)

    implementation(kotlin("test-junit5"))
    implementation(libs.junit.jupiterApi)

    val dokkaVersion = project.version.toString()
    // We're using Gradle included-builds and dependency substitution, so we
    // need to use the Gradle project name, *not* the published Maven artifact-id
    devPublication("org.jetbrains.dokka:plugin-all-modules-page:$dokkaVersion")
    devPublication("org.jetbrains.dokka:analysis-kotlin-api:$dokkaVersion")
    devPublication("org.jetbrains.dokka:analysis-kotlin-descriptors:$dokkaVersion")
    devPublication("org.jetbrains.dokka:analysis-kotlin-symbols:$dokkaVersion")
    devPublication("org.jetbrains.dokka:analysis-markdown-jb:$dokkaVersion")
    devPublication("org.jetbrains.dokka:plugin-android-documentation:$dokkaVersion")
    devPublication("org.jetbrains.dokka:plugin-base:$dokkaVersion")
    devPublication("org.jetbrains.dokka:plugin-base-test-utils:$dokkaVersion")
    devPublication("org.jetbrains.dokka:dokka-core:$dokkaVersion")
    devPublication("org.jetbrains.dokka:plugin-gfm:$dokkaVersion")
    devPublication("org.jetbrains.dokka:plugin-gfm-template-processing:$dokkaVersion")
    devPublication("org.jetbrains.dokka:plugin-javadoc:$dokkaVersion")
    devPublication("org.jetbrains.dokka:plugin-jekyll:$dokkaVersion")
    devPublication("org.jetbrains.dokka:plugin-jekyll-template-processing:$dokkaVersion")
    devPublication("org.jetbrains.dokka:plugin-kotlin-as-java:$dokkaVersion")
    devPublication("org.jetbrains.dokka:plugin-mathjax:$dokkaVersion")
    devPublication("org.jetbrains.dokka:plugin-templating:$dokkaVersion")
    devPublication("org.jetbrains.dokka:plugin-versioning:$dokkaVersion")

    devPublication("org.jetbrains.dokka:runner-maven-plugin:$dokkaVersion")
}

val templateProjectsDir = layout.projectDirectory.dir("projects")

val dokkaSubprojects = gradle.includedBuild("dokka")
val mavenPlugin = gradle.includedBuild("runner-maven-plugin")

tasks.integrationTest {
    dependsOn(checkoutBioJava)

    dependsOn(tasks.installMavenBinary)
    val mvn = mavenCliSetup.mvn
    inputs.file(mvn)

    val dokkaVersion = provider { project.version.toString() }
    inputs.property("dokkaVersion", dokkaVersion)

    doFirst("workaround for https://github.com/gradle/gradle/issues/24267") {
        environment("DOKKA_VERSION", dokkaVersion.get())
        environment("MVN_BINARY_PATH", mvn.get().asFile.invariantSeparatorsPath)
    }

    devMavenPublish.configureTask(this)
}

val checkoutBioJava by tasks.registering(GitCheckoutTask::class) {
    uri = "https://github.com/biojava/biojava.git"
    commitId = "059fbf1403d0704801df1427b0ec925102a645cd"
    destination = templateProjectsDir.dir("biojava/biojava")
}
