import org.jetbrains.CrossPlatformExec
import org.jetbrains.SetupMaven
import org.jetbrains.registerDokkaArtifactPublication

val setupMaven by tasks.register<SetupMaven>("setupMaven")

dependencies {
    implementation(project(":core"))
    implementation("org.apache.maven:maven-core:${setupMaven.mavenVersion}")
    implementation("org.apache.maven:maven-plugin-api:${setupMaven.mavenVersion}")
    implementation("org.apache.maven.plugin-tools:maven-plugin-annotations:${setupMaven.mavenPluginToolsVersion}")
    implementation("org.apache.maven:maven-archiver:2.5")
    implementation(kotlin("stdlib-jdk8"))
}

tasks.named<Delete>("clean") {
    delete(setupMaven.mavenBuildDir)
    delete(setupMaven.mavenBinDir)
}

/**
 * Generate pom.xml for Maven Plugin Plugin
 */
val generatePom by tasks.registering(Copy::class) {
    val dokka_version: String by project
    inputs.property("dokka_version", dokka_version)

    from("$projectDir/pom.tpl.xml") {
        rename("(.*).tpl.xml", "$1.xml")
    }
    into(setupMaven.mavenBuildDir)

    eachFile {
        filter { line ->
            line.replace("<maven.version></maven.version>", "<maven.version>${setupMaven.mavenVersion}</maven.version>")
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

/**
 * Copy compiled classes to [mavenBuildDir] for Maven Plugin Plugin
 */
val syncClasses by tasks.registering(Sync::class) {
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
}

val pluginDescriptor by tasks.registering(CrossPlatformExec::class) {
    dependsOn(setupMaven, generatePom, syncClasses)
    workingDir(setupMaven.mavenBuildDir)
    commandLine(setupMaven.mvn, "-e", "-B", "org.apache.maven.plugins:maven-plugin-plugin:descriptor")
}

val sourceJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
}

tasks.named<Jar>("jar") {
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
