/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it.gradle.junit


sealed interface TestedVersion {
    val version: String

    data class Gradle(override val version: String) : TestedVersion

    data class KotlinGradlePlugin(override val version: String) : TestedVersion

    data class AndroidGradlePlugin(override val version: String) : TestedVersion

    data class ComposeVersion(override val version: String) : TestedVersion

    companion object
}

val TestedVersion.Companion.Gradle_V_7_6_3
    get() = TestedVersion.Gradle("7.6.3")

val TestedVersion.Companion.Gradle_V_7_6_4
    get() = TestedVersion.Gradle("7.6.4")

/** The latest Gradle v7.6 version (currently 7.6.4). */
val TestedVersion.Companion.Gradle_V_7_6_Latest
    get() = TestedVersion.Gradle_V_7_6_4


fun interface TestedVersionCompatibilityRule {
    fun evaluate(version1: TestedVersion, version2: TestedVersion): Outcome

    enum class Outcome {
        Reject,
        Accept,
        Skip,
    }

    object RuleScope {
        val Skip get() = Outcome.Skip
        val Accept get() = Outcome.Accept
        val Reject get() = Outcome.Reject
    }

    companion object
}


inline fun <reified V1 : TestedVersion, reified V2 : TestedVersion> testedVersionCompatibilityRule(
    crossinline evaluate: TestedVersionCompatibilityRule.RuleScope.(v1: V1, v2: V2) -> TestedVersionCompatibilityRule.Outcome
): TestedVersionCompatibilityRule =
    TestedVersionCompatibilityRule { v1, v2 ->
        if (v1 is V1 && v2 is V2) {
            TestedVersionCompatibilityRule.RuleScope.evaluate(v1, v2)
        } else {
            TestedVersionCompatibilityRule.Outcome.Skip
        }
    }

val agpGradleCompat =
    testedVersionCompatibilityRule<TestedVersion.AndroidGradlePlugin, TestedVersion.Gradle> { agp, gradle ->
//
//                    AndroidGradlePluginVersion.V7_4 -> other >= GradleVersion.V7_5
//                    AndroidGradlePluginVersion.V8_0 -> other >= GradleVersion.V8_0
//                    AndroidGradlePluginVersion.V8_1 -> other >= GradleVersion.V8_0
//                    AndroidGradlePluginVersion.V8_2 -> other >= GradleVersion.V8_2
//                    AndroidGradlePluginVersion.V8_3 -> other >= GradleVersion.V8_4
//                    AndroidGradlePluginVersion.V8_4 -> other >= GradleVersion.V8_6
//                    AndroidGradlePluginVersion.V8_5 -> other >= GradleVersion.V8_7
//                    AndroidGradlePluginVersion.V8_6 -> other >= GradleVersion.V8_7
//                    AndroidGradlePluginVersion.V8_7 -> other >= GradleVersion.V8_9
        Skip
    }

//val TestedVersion.majorVersion: Int
//    get() = version.substringBefore(".").toInt()
//
//
///**
// * JVM version used to run Gradle.
// */
//enum class GradleDaemonJvmVersion : TestedVersion {
//    V8,
//    V11,
//    V17,
//    ;
//
//    override val version: String
//        get() = name.removePrefix("V")
//}
//
//enum class GradleVersion(
//    override val version: String
//) : TestedVersion {
//    V7_0("7.0.2"),
//    V7_1("7.1.1"),
//    V7_2("7.2"),
//    V7_3("7.3.3"),
//    V7_4("7.4.2"),
//    V7_5("7.5.1"),
//    V7_6("7.6.4"),
//    V8_0("8.0.2"),
//    V8_1("8.1.1"),
//    V8_2("8.2.1"),
//    V8_3("8.3"),
//    V8_4("8.4"),
//    V8_5("8.5"),
//    V8_6("8.6"),
//    V8_7("8.7"),
//    V8_8("8.8"),
//    V8_9("8.9"),
//    V8_10("8.10.2"),
//    ;
//}
//
//
//enum class KotlinGradlePluginVersion : TestedVersion {
//    V1_9_0,
//    V1_9_10,
//    V1_9_20,
//    V1_9_21,
//    V1_9_22,
//    V1_9_23,
//    V1_9_24,
//    V1_9_25,
//    V2_0_0,
//    V2_0_10,
//    V2_0_20,
//    V2_0_21,
//    ;
//
//    override val version: String
//        get() = name.removePrefix("V").replace('_', '.')
//}
//
//
//enum class AndroidGradlePluginVersion(override val version: String) : TestedVersion {
//    //V7_0("7.0.4"),
//    //V7_1("7.1.3"),
//    //V7_2("7.2.2"),
//    //V7_3("7.3.1"),
//    V7_4("7.4.2"),
//    V8_0("8.0.2"),
//    V8_1("8.1.4"),
//    V8_2("8.2.2"),
//    V8_3("8.3.2"),
//    V8_4("8.4.2"),
//    V8_5("8.5.2"),
//    V8_6("8.6.1"),
//    V8_7("8.7.1"),
//    ;
//}
//
//enum class ComposeVersion(
//    val requiredKotlinVersion: KotlinGradlePluginVersion,
//) : TestedVersion {
//    V1_5_0(KotlinGradlePluginVersion.V1_9_0),
//    V1_5_1(KotlinGradlePluginVersion.V1_9_0),
//    V1_5_2(KotlinGradlePluginVersion.V1_9_0),
//    V1_5_3(KotlinGradlePluginVersion.V1_9_10),
//    V1_5_4(KotlinGradlePluginVersion.V1_9_20),
//    V1_5_5(KotlinGradlePluginVersion.V1_9_20),
//    V1_5_6(KotlinGradlePluginVersion.V1_9_21),
//    V1_5_7(KotlinGradlePluginVersion.V1_9_21),
//    V1_5_8(KotlinGradlePluginVersion.V1_9_22),
//    V1_5_9(KotlinGradlePluginVersion.V1_9_22),
//    V1_5_10(KotlinGradlePluginVersion.V1_9_22),
//    V1_5_11(KotlinGradlePluginVersion.V1_9_23),
//    V1_5_12(KotlinGradlePluginVersion.V1_9_23),
//    V1_5_13(KotlinGradlePluginVersion.V1_9_23),
//    V1_5_14(KotlinGradlePluginVersion.V1_9_24),
//    ;
//
//    override val version: String
//        get() = name.removePrefix("V").replace('_', '.')
//}
//
//enum class ComposeCompilerGradlePluginVersion : TestedVersion {
//    V2_0_0,
//    V2_0_10,
//    V2_0_21,
//    ;
//
//    override val version: String
//        get() = name.removePrefix("V").replace('_', '.')
//}
//
//
//class CompatibilityCheckContext(
//    val version: TestedVersion,
//    val other: TestedVersion,
//) {
//    private val _incompatibilities: MutableList<String> = mutableListOf()
//    val incompatibilities: List<String> get() = _incompatibilities.toList()
//
//    fun incompatible(because: String) {
//        _incompatibilities += because
//    }
//}
//
//fun CompatibilityCheckContext.checkCompatibility() {
//    when (version) {
//        is AndroidGradlePluginVersion -> {
//            if (other is GradleVersion) {
//                // The major version of AGP is tied to the major version of Gradle.
//                // https://developer.android.com/build/releases/gradle-plugin?buildsystem=ndk-build#updating-gradle
//                val agpSupportsGradle: Boolean = when (version) {
//                    AndroidGradlePluginVersion.V7_4 -> other >= GradleVersion.V7_5
//                    AndroidGradlePluginVersion.V8_0 -> other >= GradleVersion.V8_0
//                    AndroidGradlePluginVersion.V8_1 -> other >= GradleVersion.V8_0
//                    AndroidGradlePluginVersion.V8_2 -> other >= GradleVersion.V8_2
//                    AndroidGradlePluginVersion.V8_3 -> other >= GradleVersion.V8_4
//                    AndroidGradlePluginVersion.V8_4 -> other >= GradleVersion.V8_6
//                    AndroidGradlePluginVersion.V8_5 -> other >= GradleVersion.V8_7
//                    AndroidGradlePluginVersion.V8_6 -> other >= GradleVersion.V8_7
//                    AndroidGradlePluginVersion.V8_7 -> other >= GradleVersion.V8_9
//                }
//
//                if (!agpSupportsGradle) {
//                    incompatible("Each AGP has a limited range of supported Gradle versions")
//                }
//            }
//
//            if (other is GradleDaemonJvmVersion) {
//
//                val agpSupportsGradleDaemonJvm: Boolean = when (version) {
//                    AndroidGradlePluginVersion.V7_4 -> other >= GradleDaemonJvmVersion.V11
//                    AndroidGradlePluginVersion.V8_0,
//                    AndroidGradlePluginVersion.V8_1,
//                    AndroidGradlePluginVersion.V8_2,
//                    AndroidGradlePluginVersion.V8_3,
//                    AndroidGradlePluginVersion.V8_4,
//                    AndroidGradlePluginVersion.V8_5,
//                    AndroidGradlePluginVersion.V8_6,
//                    AndroidGradlePluginVersion.V8_7 -> other >= GradleDaemonJvmVersion.V17
//                }
//
//                if (!agpSupportsGradleDaemonJvm) {
//                    incompatible("AGP has a minimum JDK requirement.")
//                }
//            }
//        }
//
//        is GradleVersion -> {
//
//        }
//
//        is GradleDaemonJvmVersion -> {
//
//        }
//
//        is KotlinGradlePluginVersion -> {
//
//        }
//
//        is ComposeCompilerGradlePluginVersion -> {
//            if (other is KotlinGradlePluginVersion) {
//                if (version.version != other.version) {
//                    incompatible("Compose Compiler Gradle plugin must have the same version as KGP")
//                }
//            }
//        }
//
//        is ComposeVersion -> {
//            if (other is KotlinGradlePluginVersion) {
//                // Compose has very strict requirements on the Kotlin version.
//                // https://developer.android.com/jetpack/androidx/releases/compose-kotlin#pre-release_kotlin_compatibility
//                if (version.requiredKotlinVersion != other) {
//                    incompatible("Compose has strict version requirements")
//                }
//            }
//        }
//    }
//}
