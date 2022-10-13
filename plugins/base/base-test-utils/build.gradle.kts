import org.jetbrains.registerDokkaArtifactPublication

plugins {
    org.jetbrains.conventions.`kotlin-jvm`
    org.jetbrains.conventions.`maven-publish`
}

dependencies {
    compileOnly(project(":plugins:base"))
    implementation(project(":core:test-api"))
    val jsoup_version: String by project
    implementation("org.jsoup:jsoup:$jsoup_version")
    implementation(kotlin("test-junit"))
}

registerDokkaArtifactPublication("dokkaBaseTestUtils") {
    artifactId = "dokka-base-test-utils"
}
