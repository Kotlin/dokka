import org.jetbrains.configurePublication

plugins {
    `maven-publish`
    id("com.jfrog.bintray")
}

dependencies {
    api(project(":core"))
    implementation(project(":kotlin-analysis"))
    implementation("junit:junit:4.13") // TODO: remove dependency to junit
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
}

configurePublication("dokka-test-api")
