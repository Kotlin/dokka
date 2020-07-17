import org.jetbrains.dependsOnMavenLocalPublication

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("test-junit"))
    implementation(gradleTestKit())
}

tasks.integrationTest {
    inputs.dir(file("projects"))
    dependsOnMavenLocalPublication()
}

tasks.clean {
    delete(File(buildDir, "gradle-test-kit"))
}
