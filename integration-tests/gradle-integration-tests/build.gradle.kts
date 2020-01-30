val dokkaPlugin by configurations.creating
val dokkaCore by configurations.creating
val kotlinGradle by configurations.creating

dependencies {
    val kotlin_version: String by project
    testCompileOnly(group = "org.jetbrains.kotlin", name = "kotlin-stdlib", version = kotlin_version)
    testImplementation(
        group = "org.jetbrains.kotlin",
        name = "kotlin-test-junit",
        version = kotlin_version
    )
    testImplementation(project(":coreDependencies"))
    dokkaPlugin(project(path = ":runners:gradle-plugin"))
    dokkaCore(project(path = ":core", configuration = "shadow"))

    kotlinGradle("org.jetbrains.kotlin:kotlin-gradle-plugin")

    testImplementation(group = "junit", name = "junit", version = "4.13")
    testImplementation(gradleTestKit())
}


val createClasspathManifest by tasks.registering {
    dependsOn(project(":core").getTasksByName("shadowJar", true))

    val outputDir = file("$buildDir/$name")
    inputs.files(dokkaPlugin + dokkaCore)
    outputs.dir(outputDir)

    doLast {
        outputDir.mkdirs()
        file("$outputDir/dokka-plugin-classpath.txt").writeText(dokkaPlugin.joinToString("\n"))
        file("$outputDir/fatjar.txt").writeText(dokkaCore.joinToString("\n"))
        file("$outputDir/kotlin-gradle.txt").writeText(kotlinGradle.joinToString("\n"))
    }
}

val testClasses by tasks.getting

testClasses.dependsOn(project(":core").getTasksByName("shadowJar", true))
testClasses.dependsOn(createClasspathManifest)

tasks {
    test {
        systemProperty("android.licenses.overwrite", project.findProperty("android.licenses.overwrite") ?: "")
        inputs.dir(file("testData"))
        exclude("*") // TODO: Remove this exclude when tests are migrated
    }
}

// TODO: see if this file makes any sense