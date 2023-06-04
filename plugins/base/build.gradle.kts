import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
    id("org.jetbrains.conventions.maven-publish")
    id("org.jetbrains.conventions.dokka-html-frontend-files")
}

dependencies {
    compileOnly(projects.core)

    implementation(kotlin("reflect"))

    implementation(libs.kotlinx.coroutines.core)

    compileOnly(projects.kotlinAnalysis)
    implementation(libs.jsoup)

    implementation(libs.jackson.kotlin)
    constraints {
        implementation(libs.jackson.databind) {
            because("CVE-2022-42003")
        }
    }

    implementation(libs.freemarker)

    testImplementation(projects.plugins.base.baseTestUtils)
    testImplementation(projects.core.contentMatcherTestUtils)

    implementation(libs.kotlinx.html)

    testImplementation(projects.kotlinAnalysis)
    testImplementation(projects.core.testApi)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)

    dokkaHtmlFrontendFiles(projects.plugins.base.frontend) {
        because("fetch frontend files from subproject :plugins:base:frontend")
    }
}

// access the frontend files via the dependency on :plugins:base:frontend
val dokkaHtmlFrontendFiles: Provider<FileCollection> =
    configurations.dokkaHtmlFrontendFiles.map { frontendFiles ->
        frontendFiles.incoming.artifacts.artifactFiles
    }

val prepareDokkaHtmlFrontendFiles by tasks.registering(Sync::class) {
    description = "copy Dokka Base frontend files into the resources directory"

    from(dokkaHtmlFrontendFiles) {
        include("*.js")
        into("dokka/scripts")
    }

    from(dokkaHtmlFrontendFiles) {
        include("*.css")
        into("dokka/styles")
    }

    into(layout.buildDirectory.dir("generated/src/main/resources"))
}

sourceSets.main {
    resources.srcDir(prepareDokkaHtmlFrontendFiles.map { it.destinationDir })
}

tasks.test {
    maxHeapSize = "4G"
}

registerDokkaArtifactPublication("dokka-base")
