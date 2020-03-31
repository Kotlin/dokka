publishing {
    publications {
        register<MavenPublication>("gfmPlugin") {
            artifactId = "gfm-plugin"
            from(components["java"])
        }
    }
}

dependencies {
    implementation(project(":plugins:base"))
}
