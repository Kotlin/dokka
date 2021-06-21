import org.jetbrains.dependsOnMavenLocalPublication

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("test-junit"))
    implementation(gradleTestKit())

    val jsoup_version: String by project
    implementation("org.jsoup:jsoup:$jsoup_version")
}

tasks.integrationTest {
    val dokka_version: String by project
    environment("DOKKA_VERSION", dokka_version)
    inputs.dir(file("projects"))
    dependsOnMavenLocalPublication()
}

tasks.clean {
    delete(File(buildDir, "gradle-test-kit"))
}
