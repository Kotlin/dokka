val dokkaPlugin: Configuration by configurations.creating
val dokkaCore: Configuration by configurations.creating
val kotlinGradle: Configuration by configurations.creating

repositories {
    maven(url = "https://kotlin.bintray.com/kotlin-plugin")
}

dependencies {
    val kotlin_version: String by project
    testCompileOnly("org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    testImplementation("junit:junit:4.13")
    testImplementation(gradleTestKit())

    dokkaPlugin(project(path = ":runners:gradle-plugin"))
    dokkaCore(project(path = ":core", configuration = "shadow"))
    kotlinGradle("org.jetbrains.kotlin:kotlin-gradle-plugin")
}

val createClasspathManifest by tasks.registering {
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

tasks {
    testClasses {
        dependsOn(createClasspathManifest)
    }

    test {
        systemProperty("android.licenses.overwrite", project.findProperty("android.licenses.overwrite") ?: "")
        inputs.dir(file("testData"))
//        exclude("*") // TODO: Remove this exclude when tests are migrated
    }
}
