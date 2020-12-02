dependencies {
    compileOnly(project(":plugins:base"))
    implementation(project(":core:test-api"))
    implementation("org.jsoup:jsoup:1.12.1")
    implementation(kotlin("test-junit"))
}
