import org.jetbrains.CrossPlatformExec
import org.jetbrains.registerDokkaArtifactPublication

plugins {
    org.jetbrains.conventions.`kotlin-jvm`
    org.jetbrains.conventions.`maven-publish`
    org.jetbrains.conventions.`maven-cli-setup`
}

val pomTemplateFile = layout.projectDirectory.file("pom.tpl.xml")

dependencies {
    implementation(project(":core"))
    implementation("org.apache.maven:maven-core:${setupMavenProperties.mavenVersion}")
    implementation("org.apache.maven:maven-plugin-api:${setupMavenProperties.mavenVersion}")
    implementation("org.apache.maven.plugin-tools:maven-plugin-annotations:${setupMavenProperties.mavenPluginToolsVersion}")
    implementation("org.apache.maven:maven-archiver:2.5")
    implementation(kotlin("stdlib-jdk8"))
}

val generatePom by tasks.registering(Sync::class) {
    description = "Generate pom.xml for Maven Plugin Plugin"

    val dokka_version: String by project
    inputs.property("dokka_version", dokka_version)

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

val prepareCompiledClasses by tasks.registering(Sync::class) {
    description = "Prepare compiled classes for Maven Plugin task execution"

    from(tasks.compileKotlin.flatMap { it.destinationDirectory })
    from(tasks.compileJava.flatMap { it.destinationDirectory })
    into(temporaryDir)
}

val prepareMavenPluginBuildDir by tasks.registering(Sync::class) {
    description = "Prepare files for Maven Plugin task execution"
    from(prepareCompiledClasses) { into("classes/java/main") }
    from(generatePom)
    into(setupMavenProperties.mavenBuildDir)
}

val helpMojo by tasks.registering(CrossPlatformExec::class) {
    dependsOn(tasks.installMavenBinary, prepareMavenPluginBuildDir)
    workingDir(setupMavenProperties.mavenBuildDir)
    executable(setupMavenProperties.mvn.get())
    args("-e", "-B", "org.apache.maven.plugins:maven-plugin-plugin:helpmojo")

    outputs.dir(setupMavenProperties.mavenBuildDir)
}

val pluginDescriptor by tasks.registering(CrossPlatformExec::class) {
    dependsOn(tasks.installMavenBinary, prepareMavenPluginBuildDir)
    workingDir(setupMavenProperties.mavenBuildDir)
    executable(setupMavenProperties.mvn.get())
    args("-e", "-B", "org.apache.maven.plugins:maven-plugin-plugin:descriptor")

    outputs.dir(layout.buildDirectory.dir("maven/classes/java/main/META-INF/maven"))
}

tasks.jar {
    dependsOn(pluginDescriptor, helpMojo)
    metaInf {
        from("${setupMavenProperties.mavenBuildDir}/classes/java/main/META-INF")
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
