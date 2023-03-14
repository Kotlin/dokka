import org.jetbrains.CrossPlatformExec
import org.jetbrains.SetupMaven
import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
    id("org.jetbrains.conventions.maven-publish")
}

val setupMaven by tasks.register<SetupMaven>("setupMaven")

dependencies {
    implementation(projects.core)

    implementation(libs.apache.mavenCore)
    implementation(libs.apache.mavenPluginApi)
    implementation(libs.apache.mavenPluginAnnotations)
    implementation(libs.apache.mavenArchiver)


}

val mavenBuildDir = setupMaven.mavenBuildDir
val mavenBinDir = setupMaven.mavenBinDir

tasks.clean {
    delete(mavenBuildDir)
    delete(mavenBinDir)
}

val generatePom by tasks.registering(Copy::class) {
    description = "Generate pom.xml for Maven Plugin Plugin"

    val dokka_version: String by project
    inputs.property("dokka_version", dokka_version)

    from("$projectDir/pom.tpl.xml") {
        rename("(.*).tpl.xml", "$1.xml")
    }
    into(setupMaven.mavenBuildDir)

    eachFile {
        filter { line ->
            line.replace(
                "<maven.version></maven.version>",
                "<maven.version>${setupMaven.mavenVersion}</maven.version>"
            )
        }
        filter { line ->
            line.replace("<version>dokka_version</version>", "<version>$dokka_version</version>")
        }
        filter { line ->
            line.replace(
                "<version>maven-plugin-plugin</version>",
                "<version>${setupMaven.mavenPluginToolsVersion}</version>"
            )
        }
    }
}

val syncClasses by tasks.registering(Sync::class) {
    description = "Copy compiled classes to the Maven build dir, for Maven Plugin task execution"

    dependsOn(tasks.compileKotlin, tasks.compileJava)
    from("$buildDir/classes/kotlin", "$buildDir/classes/java")
    into("${setupMaven.mavenBuildDir}/classes/java")

    preserve {
        include("**/*.class")
    }
}

val helpMojo by tasks.registering(CrossPlatformExec::class) {
    dependsOn(setupMaven, generatePom, syncClasses)
    workingDir(setupMaven.mavenBuildDir)
    commandLine(setupMaven.mvn, "-e", "-B", "org.apache.maven.plugins:maven-plugin-plugin:helpmojo")

    outputs.dir(layout.buildDirectory.dir("maven"))
}

val pluginDescriptor by tasks.registering(CrossPlatformExec::class) {
    dependsOn(setupMaven, generatePom, syncClasses)
    workingDir(setupMaven.mavenBuildDir)
    commandLine(
        setupMaven.mvn,
        "-e",
        "-B",
        "org.apache.maven.plugins:maven-plugin-plugin:descriptor"
    )

    outputs.dir(layout.buildDirectory.dir("maven/classes/java/main/META-INF/maven"))
}

tasks.jar {
    dependsOn(pluginDescriptor, helpMojo)
    metaInf {
        from("${setupMaven.mavenBuildDir}/classes/java/main/META-INF")
    }
    manifest {
        attributes("Class-Path" to configurations.runtimeClasspath.map { configuration ->
            configuration.resolve().joinToString(" ") { it.name }
        })
    }
    duplicatesStrategy = DuplicatesStrategy.WARN
}


registerDokkaArtifactPublication("dokkaMavenPlugin") {
    artifactId = "dokka-maven-plugin"
}
