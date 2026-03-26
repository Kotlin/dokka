import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm")
    application
    id("com.github.johnrengelman.shadow") version "8.1.1" // Plugin to build a fat JAR
}

repositories {
    mavenCentral()
}

dependencies {
    // new versions of GraalJS require Java 17+.
    // For Java 8 use older versions (the last one supporting Java 8 is 21.0.0.2)
    implementation("org.graalvm.sdk:graal-sdk:21.0.0.2")
    implementation("org.graalvm.js:js:21.0.0.2")

    // Library for convenient JSON handling in CLI (optional, but useful for reading input.json)
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.0")
}

application {
    mainClass.set("MainKt")
}

// Configure the toolchain so Gradle uses Java 8 for compilation
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}


// --- 1. Frontend build configuration ---

// Task to run npm install
val npmInstall by tasks.registering(Exec::class) {
    workingDir = file("src/frontend")
    // OS check to call the correct command
    val npmCmd = if (System.getProperty("os.name").lowercase().contains("win")) "npm.cmd" else "npm"
    commandLine(npmCmd, "install")
    // So the task won't run every time if node_modules already exists
    inputs.file("src/frontend/package.json")
    outputs.dir("src/frontend/node_modules")
}

// Task to run npm run build
val npmBuild by tasks.registering(Exec::class) {
    dependsOn(npmInstall)
    workingDir = file("src/frontend")
    val npmCmd = if (System.getProperty("os.name").lowercase().contains("win")) "npm.cmd" else "npm"
    commandLine(npmCmd, "run", "build")

    inputs.dir("src/frontend/src")
    inputs.file("src/frontend/webpack.config.js")
    // The webpack result is our bundle
    outputs.file("src/main/resources/server-bundle.js")
}

// Make resource processing depend on the JS build
tasks.processResources {
    dependsOn(npmBuild)
    from("src/frontend/public/index.html")
}

tasks.named<JavaExec>("run") {
    // Pass the path to the file as an argument.
    // The input.json file should be in the project root (next to build.gradle.kts)
    args = listOf("input.json")
}


// --- 2. JAR build configuration ---

tasks.withType<ShadowJar> {
    archiveFileName.set("docgen.jar") // Name of the output JAR
    mergeServiceFiles() // Important for GraalJS so services are included
    manifest {
        attributes["Main-Class"] = "MainKt"
    }
}