plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
}

dependencies {
    implementation(project(mapOf("path" to ":core")))
    compileOnly(projects.core)
    compileOnly(projects.subprojects.analysisKotlinApi)

    listOf(
        "com.jetbrains.intellij.platform:util-rt",
        "com.jetbrains.intellij.platform:util-class-loader",
        "com.jetbrains.intellij.platform:util-text-matching",
        "com.jetbrains.intellij.platform:util",
        "com.jetbrains.intellij.platform:util-base",
        "com.jetbrains.intellij.platform:util-xml-dom",
        "com.jetbrains.intellij.platform:core",
        "com.jetbrains.intellij.platform:core-impl",
        "com.jetbrains.intellij.platform:extensions",
        "com.jetbrains.intellij.java:java-psi",
        "com.jetbrains.intellij.java:java-psi-impl"
    ).forEach {
        implementation("$it:213.7172.25") { isTransitive = false }
    }

    api("com.jetbrains.intellij.platform:core:213.7172.25")

    implementation(projects.subprojects.analysisMarkdownJb)
    implementation(projects.subprojects.analysisJavaPsi)


    implementation("org.jetbrains.kotlin:high-level-api-for-ide:1.9.0-release-358") {
        isTransitive = false // see KTIJ-19820
    }
    implementation("org.jetbrains.kotlin:high-level-api-impl-base-for-ide:1.9.0-release-358") {
        isTransitive = false // see KTIJ-19820
    }
    implementation("org.jetbrains.kotlin:high-level-api-fir-for-ide:1.9.0-release-358") {
        isTransitive = false // see KTIJ-19820
    }
    implementation("org.jetbrains.kotlin:high-level-api-fe10-for-ide:1.9.0-release-358") {
        isTransitive = false // see KTIJ-19820
    }

    implementation("org.jetbrains.kotlin:low-level-api-fir-for-ide:1.9.0-release-358") {
        isTransitive = false // see KTIJ-19820
    }
    implementation("org.jetbrains.kotlin:analysis-project-structure-for-ide:1.9.0-release-358") {
        isTransitive = false // see KTIJ-19820
    }
    implementation("org.jetbrains.kotlin:analysis-api-standalone-for-ide:1.9.0-release-358") {
        isTransitive = false // see KTIJ-19820
    }
    implementation("org.jetbrains.kotlin:analysis-api-providers-for-ide:1.9.0-release-358") {
        isTransitive = false // see KTIJ-19820
    }
    implementation("org.jetbrains.kotlin:symbol-light-classes-for-ide:1.9.0-release-358") {
        isTransitive = false
    }
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm:0.3.4")
   // implementation("com.jetbrains.intellij.platform:concurrency:203.8084.24")


    ///implementation("org.jetbrains.kotlin:kotlin-compiler:1.9.0")
    implementation("org.jetbrains.kotlin:kotlin-compiler-for-ide:1.9.0-release-358") {
        isTransitive = false
    }



    // TODO [beresnev] get rid of it
    compileOnly(libs.kotlinx.coroutines.core)
}
