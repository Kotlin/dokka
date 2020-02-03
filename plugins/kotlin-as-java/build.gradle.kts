publishing {
    publications {
        register<MavenPublication>("kotlin-as-java-plugin") {
            artifactId = "kotlin-as-java-plugin"
            from(components["java"])
        }
    }
}

val kotlin_version: String by project
dependencies {
    implementation(kotlin("stdlib-jdk8"))
    compileOnly(project(":coreDependencies", configuration = "shadow"))
    testCompileOnly(project(":core"))
    testCompileOnly(project(":coreDependencies", configuration = "shadow"))
    testImplementation("junit:junit:4.12")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}