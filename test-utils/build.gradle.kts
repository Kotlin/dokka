plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
}

dependencies {
    api(kotlin("test-junit"))

    api(platform("org.junit:junit-bom:5.9.2"))
    // support both JUnit 4 and JUnit 5 tests
    api("org.junit.jupiter:junit-jupiter-engine")
    api("org.junit.vintage:junit-vintage-engine")
}
