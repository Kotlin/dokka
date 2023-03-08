import org.gradle.kotlin.dsl.support.appendReproducibleNewLine
import org.jetbrains.CrossPlatformExec
import org.jetbrains.registerDokkaArtifactPublication

plugins {
    org.jetbrains.conventions.`kotlin-jvm`
    org.jetbrains.conventions.`maven-publish`
    org.jetbrains.conventions.`maven-cli-setup`
}

dependencies {
    implementation(project(":core"))
    implementation("org.apache.maven:maven-core:${setupMavenProperties.mavenVersion}")
    implementation("org.apache.maven:maven-plugin-api:${setupMavenProperties.mavenVersion}")
    implementation("org.apache.maven.plugin-tools:maven-plugin-annotations:${setupMavenProperties.mavenPluginToolsVersion}")
    implementation("org.apache.maven:maven-archiver:2.5")
    implementation(kotlin("stdlib-jdk8"))
}

val mavenPluginTaskGroup = "maven plugin"

val generatePom by tasks.registering(Sync::class) {
    description = "Generate pom.xml for Maven Plugin Plugin"
    group = mavenPluginTaskGroup

    val dokka_version: String by project
    inputs.property("dokka_version", dokka_version)

    val pomTemplateFile = layout.projectDirectory.file("pom.tpl.xml")

    from(pomTemplateFile) {
        rename { it.replace(".tpl.xml", ".xml") }

        expand(
            "mavenVersion" to setupMavenProperties.mavenVersion,
            "dokka_version" to dokka_version,
            "mavenPluginToolsVersion" to setupMavenProperties.mavenPluginToolsVersion,
        )
    }

    into(temporaryDir)
}

val prepareMavenPluginBuildDir by tasks.registering(Sync::class) {
    description = "Prepares all files for Maven Plugin task execution"
    group = mavenPluginTaskGroup

    from(tasks.compileKotlin.flatMap { it.destinationDirectory }) { into("classes/java/main") }
    from(tasks.compileJava.flatMap { it.destinationDirectory }) { into("classes/java/main") }

    from(generatePom)

    into(setupMavenProperties.mavenBuildDir)
}

val helpMojo by tasks.registering(CrossPlatformExec::class) {
    group = mavenPluginTaskGroup

    dependsOn(tasks.installMavenBinary, prepareMavenPluginBuildDir)
    workingDir(setupMavenProperties.mavenBuildDir)
    executable(setupMavenProperties.mvn.get())
    args("-e", "-B", "org.apache.maven.plugins:maven-plugin-plugin:helpmojo")

    outputs.dir(setupMavenProperties.mavenBuildDir)

    doLast("normalize maven-plugin-help.properties") {
        // The maven-plugin-help.properties file contains a timestamp by default.
        // It should be removed as it is not reproducible and impacts Gradle caching
        val pluginHelpProperties = workingDir.resolve("maven-plugin-help.properties")
        pluginHelpProperties.writeText(
            buildString {
                val lines = pluginHelpProperties.readText().lines().iterator()
                // the first line is a descriptive comment
                appendReproducibleNewLine(lines.next())
                // the second line is the timestamp, which should be ignored
                lines.next()
                // the remaining lines are properties
                lines.forEach { appendReproducibleNewLine(it) }
            }
        )
    }
}

val pluginDescriptor by tasks.registering(CrossPlatformExec::class) {
    group = mavenPluginTaskGroup

    dependsOn(tasks.installMavenBinary, prepareMavenPluginBuildDir)
    workingDir(setupMavenProperties.mavenBuildDir)
    executable(setupMavenProperties.mvn.get())
    args("-e", "-B", "org.apache.maven.plugins:maven-plugin-plugin:descriptor")

    outputs.dir(layout.buildDirectory.dir("maven/classes/java/main/META-INF/maven"))
}

tasks.jar {
    dependsOn(pluginDescriptor, helpMojo)
    metaInf {
        from(setupMavenProperties.mavenBuildDir.map { it.dir("classes/java/main/META-INF") })
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
