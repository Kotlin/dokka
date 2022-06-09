import org.jetbrains.registerDokkaArtifactPublication

dependencies {
    compileOnly(project(":kotlin-analysis"))
    implementation(project(":plugins:base"))
    testImplementation(project(":plugins:base"))
    testImplementation(project(":plugins:base:base-test-utils"))
    testImplementation(project(":core:content-matcher-test-utils"))
    val jsoup_version: String by project
    testImplementation("org.jsoup:jsoup:$jsoup_version")
    testImplementation(project(":kotlin-analysis"))
}

registerDokkaArtifactPublication("kotlinAsJavaPlugin") {
    artifactId = "kotlin-as-java-plugin"
}
