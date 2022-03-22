import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("com.github.johnrengelman.shadow")
    `maven-publish`
}

dependencies {
    compileOnly(project(":core"))
    api(project("intellij-dependency", configuration = "shadow"))
    api(project("compiler-dependency", configuration = "shadow"))
}

registerDokkaArtifactPublication("dokkaAnalysis") {
    artifactId = "dokka-analysis"
}
