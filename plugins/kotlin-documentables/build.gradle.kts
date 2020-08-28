import org.jetbrains.registerDokkaArtifactPublication

registerDokkaArtifactPublication("dokkaKotlinDocumetables") {
    artifactId = "dokka-kotlin-documentables"
}

dependencies {
    api(project(":plugins:kotlin-analysis"))
    implementation(project(":plugins:parsers"))
}