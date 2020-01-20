publishing {
    publications {
        register<MavenPublication>("kotlin-as-java-plugin") {
            artifactId = "kotlin-as-java-plugin"
            from(components["java"])
        }
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    compileOnly(project(":coreDependencies", configuration = "shadow"))
    testImplementation(project(":core"))
    testImplementation(project(":coreDependencies", configuration = "shadow"))
    testImplementation(project(":testApi"))
    testImplementation("junit:junit:4.13")
}