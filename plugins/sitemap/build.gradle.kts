import org.jetbrains.registerDokkaArtifactPublication

dependencies {
    implementation(project(":plugins:templating"))
    implementation(project(":plugins:base"))
    compileOnly(project(":plugins:all-modules-page"))

    testImplementation(project(":plugins:base"))
    testImplementation(project(":plugins:base:base-test-utils"))
    testImplementation(project(":core:content-matcher-test-utils"))
    testImplementation(kotlin("test-junit"))
}

registerDokkaArtifactPublication("sitemapPlugin") {
    artifactId = "sitemap-plugin"
}
