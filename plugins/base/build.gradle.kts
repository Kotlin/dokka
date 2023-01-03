import org.jetbrains.registerDokkaArtifactPublication


dependencies {
    val coroutines_version: String by project
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")

    compileOnly(project(":kotlin-analysis"))
    val jsoup_version: String by project
    implementation("org.jsoup:jsoup:$jsoup_version")

    val jackson_version: String by project
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jackson_version")
    val jackson_databind_version: String by project
    constraints {
        implementation("com.fasterxml.jackson.core:jackson-databind:$jackson_databind_version") {
            because("CVE-2022-42003")
        }
    }

    testImplementation(project(":plugins:base:base-test-utils"))
    testImplementation(project(":plugins:html"))

    testImplementation(project(":core:content-matcher-test-utils"))

    testImplementation(project(":kotlin-analysis"))
}

registerDokkaArtifactPublication("dokkaBase") {
    artifactId = "dokka-base"
}
