plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
    id("org.jetbrains.conventions.maven-publish")
}

repositories {
    // Override the shared repositories defined in the root settings.gradle.kts
    // These repositories are very specific and are not needed in other projects
    mavenCentral()
    maven("https://www.jetbrains.com/intellij-repository/releases")
    maven("https://cache-redirector.jetbrains.com/intellij-dependencies")

    maven("https://www.jetbrains.com/intellij-repository/snapshots") {
        mavenContent { snapshotsOnly() }
    }
}

//val intellijCore: Configuration by configurations.creating
//
//fun intellijCoreAnalysis() = zipTree(intellijCore.singleFile).matching {
//    include("intellij-core.jar")
//}
//
//val jpsStandalone: Configuration by configurations.creating
//
//fun jpsModel() = zipTree(jpsStandalone.singleFile).matching {
//    include("jps-model.jar")
//    include("aalto-xml-*.jar")
//}

dependencies {
    // TODO [beresnev] change to compileOnly?
    implementation(projects.core)

    implementation(projects.analysis.javaAnalysisApi)

    // TODO [beresnev] remove
    implementation(libs.kotlinx.coroutines.core)

    // TODO [beresnev] add a description for why this is needed
    implementation(libs.jsoup)

//    @Suppress("UnstableApiUsage")
//    intellijCore(libs.jetbrainsIntelliJ.core)
//    implementation(intellijCoreAnalysis())
//
//    @Suppress("UnstableApiUsage")
//    jpsStandalone(libs.jetbrainsIntelliJ.jpsStandalone)
//    implementation(jpsModel())

//    TODO [beresnev] uncomment once migration to 221+ is done
    val idea_version = "221.5591.52"
    implementation("com.jetbrains.intellij.java:java-psi-impl:$idea_version")
    implementation("com.jetbrains.intellij.platform:util-rt:$idea_version")
    implementation("com.jetbrains.intellij.platform:jps-model-impl:$idea_version")
}
