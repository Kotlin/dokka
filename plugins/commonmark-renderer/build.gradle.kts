publishing {
    publications {
        register<MavenPublication>("commonmark-renderer-plugin") {
            artifactId = "commonmark-renderer-plugin"
            from(components["java"])
        }
    }
}