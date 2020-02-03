import org.jetbrains.configureBintrayPublication
import org.jetbrains.CrossPlatformExec
/**
 * [mavenBin] configuration is used to download Maven Plugin Plugin
 * for generating plugin-help.xml and plugin.xml files
 */
val mavenBin: Configuration by configurations.creating

val mavenVersion = "3.5.0"
val mavenPluginToolsVersion = "3.5.2"

dependencies {
    implementation(project(":core"))
    implementation("org.apache.maven:maven-core:$mavenVersion")
    implementation("org.apache.maven:maven-plugin-api:$mavenVersion")
    implementation("org.apache.maven.plugin-tools:maven-plugin-annotations:$mavenPluginToolsVersion")
    implementation("org.apache.maven:maven-archiver:2.5")
    compileOnly(kotlin("stdlib-jdk8"))

    mavenBin(group = "org.apache.maven", name = "apache-maven", version = mavenVersion, classifier = "bin", ext = "zip")
}

val mavenBinDir = "$buildDir/maven-bin"
val mavenBuildDir = "$buildDir/maven"
val mvn = File(mavenBinDir, "apache-maven-$mavenVersion/bin/mvn")

tasks.named<Delete>("clean") {
    delete(mavenBinDir)
}

/**
 * Copy Maven Plugin Plugin to [mavenBinDir] directory
 */
val setupMaven by tasks.registering(Sync::class) {
    from(mavenBin.map { zipTree(it) })
    into(mavenBinDir)
}

/**
 * Generate pom.xml for Maven Plugin Plugin
 */
val generatePom by tasks.registering(Copy::class) {
    val dokka_version: String by project

    from("$projectDir/pom.tpl.xml") {
        rename("(.*).tpl.xml", "$1.xml")
    }
    into(mavenBuildDir)

    eachFile {
        filter { line ->
            line.replace("<maven.version></maven.version>", "<maven.version>$mavenVersion</maven.version>")
        }
        filter { line ->
            line.replace("<version>dokka_version</version>", "<version>$dokka_version</version>")
        }
        filter { line ->
            line.replace("<version>maven-plugin-plugin</version>", "<version>$mavenPluginToolsVersion</version>")
        }
    }
}

/**
 * Copy compiled classes to [mavenBuildDir] for Maven Plugin Plugin
 */
val syncClasses by tasks.registering(Sync::class) {
    dependsOn(tasks.compileKotlin, tasks.compileJava)
    from("$buildDir/classes/kotlin", "$buildDir/classes/java")
    into("$mavenBuildDir/classes/java")

    preserve {
        include("**/*.class")
    }
}

val helpMojo by tasks.registering(CrossPlatformExec::class) {
    dependsOn(setupMaven, generatePom, syncClasses)
    workingDir(mavenBuildDir)
    commandLine(mvn, "-e", "-B", "org.apache.maven.plugins:maven-plugin-plugin:helpmojo")
}

val pluginDescriptor by tasks.registering(CrossPlatformExec::class) {
    dependsOn(setupMaven, generatePom, syncClasses)
    workingDir(mavenBuildDir)
    commandLine(mvn, "-e", "-B", "org.apache.maven.plugins:maven-plugin-plugin:descriptor")
}

val sourceJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
}

tasks.named<Jar>("jar") {
    dependsOn(pluginDescriptor, helpMojo)
    metaInf {
        from("$mavenBuildDir/classes/java/main/META-INF")
    }
    manifest {
        attributes("Class-Path" to configurations.runtimeClasspath.get().files.joinToString(" ") { it.name })
    }
}

publishing {
    publications {
        register<MavenPublication>("dokkaMavenPlugin") {
            artifactId = "dokka-maven-plugin"
            from(components["java"])
            artifact(sourceJar.get())
        }
    }
}

configureBintrayPublication("dokkaMavenPlugin")
