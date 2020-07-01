dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("test-junit"))
    implementation(gradleTestKit())
}

tasks {
    test {
        inputs.dir(file("projects"))
        rootProject.allprojects
            .mapNotNull { project -> project.tasks.findByName("publishToMavenLocal") }
            .forEach { publishTask -> this.dependsOn(publishTask) }
    }
}
