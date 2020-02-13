publishing {
    publications {
        register<MavenPublication>("jekyll-plugin") {
            artifactId = "jekyll-plugin"
            from(components["java"])
        }
    }
}