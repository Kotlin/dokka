import org.jetbrains.dependsOnMavenLocalPublication

plugins {
    org.jetbrains.conventions.`dokka-integration-test`
    org.jetbrains.conventions.`maven-cli-setup`
}

dependencies {
    implementation(project(":integration-tests"))
    implementation(kotlin("stdlib"))
    implementation(kotlin("test-junit"))
}

tasks.integrationTest {
    dependsOnMavenLocalPublication()

    dependsOn(tasks.installMavenBinary)
    val mvn = setupMavenProperties.mvn
    inputs.file(mvn)

    val dokka_version: String by project
    environment("DOKKA_VERSION", dokka_version)
    doFirst {
        environment("MVN_BINARY_PATH", mvn.get().asFile.invariantSeparatorsPath)
    }
}
