import org.jetbrains.invokeWhenEvaluated

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("test-junit"))
    implementation(gradleTestKit())
}

val integrationTest by tasks.integrationTest
integrationTest.inputs.dir(file("projects"))

rootProject.allprojects.forEach { otherProject ->
    otherProject.invokeWhenEvaluated { evaluatedProject ->
        evaluatedProject.tasks.findByName("publishToMavenLocal")?.let { publishingTask ->
            integrationTest.dependsOn(publishingTask)
        }
    }
}

tasks.clean {
    delete(File(buildDir, "gradle-test-kit"))
}
