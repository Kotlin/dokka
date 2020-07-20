subprojects {
    sourceSets {
        create("integrationTest") {
            compileClasspath += sourceSets.main.get().output
            runtimeClasspath += sourceSets.main.get().output
        }
    }

    configurations.getByName("integrationTestImplementation") {
        extendsFrom(configurations.implementation.get())
    }

    configurations.getByName("integrationTestRuntimeOnly") {
        extendsFrom(configurations.runtimeOnly.get())
    }

    dependencies {
        implementation(project(":integration-tests"))
    }

    val integrationTest by tasks.register<Test>("integrationTest") {
        maxHeapSize = "2G"
        description = "Runs integration tests."
        group = "verification"

        testClassesDirs = sourceSets["integrationTest"].output.classesDirs
        classpath = sourceSets["integrationTest"].runtimeClasspath

        project.properties["dokka_integration_test_parallelism"]?.toString()?.toIntOrNull()?.let { parallelism ->
            maxParallelForks = parallelism
        }

        useJUnit()
    }

    tasks.check {
        dependsOn(integrationTest)
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("test-junit"))
    val coroutines_version: String by project
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
    implementation("org.jsoup:jsoup:1.12.1")
}
