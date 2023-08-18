plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
}

dependencies {
    compileOnly(projects.core)
    compileOnly(projects.subprojects.analysisKotlinApi)

    implementation(projects.subprojects.analysisMarkdownJb)
    implementation(projects.subprojects.analysisJavaPsi)


    // ----------- IDE dependencies ----------------------------------------------------------------------------

    listOf(
        libs.intellij.platform.util.rt,
        libs.intellij.platform.util.api,
        libs.intellij.java.psi.api,
        libs.intellij.java.psi.impl
    ).forEach {
        runtimeOnly(it) { isTransitive = false }
    }

    implementation(libs.intellij.java.psi.api) { isTransitive = false }


    // TODO move to toml
    listOf(
        "com.jetbrains.intellij.platform:util-class-loader",
        "com.jetbrains.intellij.platform:util-text-matching",
        "com.jetbrains.intellij.platform:util-base",
        "com.jetbrains.intellij.platform:util-xml-dom",
        "com.jetbrains.intellij.platform:core-impl",
        "com.jetbrains.intellij.platform:extensions",
    ).forEach {
        runtimeOnly("$it:213.7172.25") { isTransitive = false }
    }

    implementation("com.jetbrains.intellij.platform:core:213.7172.25") {
        isTransitive = false
    } // for Standalone prototype

    // ----------- Analysis dependencies ----------------------------------------------------------------------------

    listOf(
        libs.kotlin.high.level.api.api,
        libs.kotlin.analysis.api.standalone,
        libs.kotlin.high.level.api.impl // for Standalone prototype
    ).forEach {
        implementation(it) {
            isTransitive = false // see KTIJ-19820
        }
    }
    listOf(
        libs.kotlin.high.level.api.impl,
        libs.kotlin.high.level.api.fir,
        libs.kotlin.high.level.api.fe10,
        libs.kotlin.low.level.api.fir,
        libs.kotlin.analysis.project.structure,
        libs.kotlin.analysis.api.providers,
        libs.kotlin.symbol.light.classes
    ).forEach {
        runtimeOnly(it) {
            isTransitive = false // see KTIJ-19820
        }
    }
    runtimeOnly(libs.kotlinx.collections.immutable)
    implementation(libs.kotlin.compiler.k2) {
        isTransitive = false
    }

    // TODO [beresnev] get rid of it
    compileOnly(libs.kotlinx.coroutines.core)
}
