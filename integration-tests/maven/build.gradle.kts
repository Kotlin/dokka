import org.jetbrains.dependsOnMavenLocalPublication

evaluationDependsOn(":runners:maven-plugin")

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("test-junit"))
}

tasks.integrationTest {
    dependsOnMavenLocalPublication()
    dependsOn(":runners:maven-plugin:setupMaven")

    val dokka_version: String by project
    environment("DOKKA_VERSION", dokka_version)
    environment("MVN_BINARY_PATH", project(":runners:maven-plugin").extra["MVN_BINARY_PATH"].toString())
}
