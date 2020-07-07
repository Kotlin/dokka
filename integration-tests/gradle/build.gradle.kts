import org.jetbrains.invokeWhenEvaluated

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("test-junit"))
    implementation(gradleTestKit())
}

tasks.integrationTest {
    inputs.dir(file("projects"))
    rootProject.allprojects.forEach { otherProject ->
        otherProject.invokeWhenEvaluated { evaluatedProject ->
            evaluatedProject.tasks.findByName("publishToMavenLocal")?.let { publishingTask ->
                this.dependsOn(publishingTask)
            }
        }
    }
}

tasks.clean {
    delete(File(buildDir, "gradle-test-kit"))
}
