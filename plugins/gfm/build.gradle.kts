publishing {
    publications {
        register<MavenPublication>("gfm-plugin") {
            artifactId = "gfm-plugin"
            from(components["java"])
        }
    }
}