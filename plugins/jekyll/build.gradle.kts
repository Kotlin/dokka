publishing {
    publications {
        register<MavenPublication>("jekyllPlugin") {
            artifactId = "jekyll-plugin"
            from(components["java"])
        }
    }
}

dependencies {
    compileOnly(project(":plugins:base"))
    implementation(project(":plugins:gfm"))
}