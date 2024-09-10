/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.it.gradle

import io.kotest.property.Arb
import io.kotest.property.Exhaustive
import io.kotest.property.arbitrary.bind
import io.kotest.property.exhaustive.of
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.extension.*
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import java.util.stream.Stream


data class Foo(val version: String)

sealed interface DokkaGradlePluginTestParameter {
    data class AndroidGradlePlugin(val version: String) : DokkaGradlePluginTestParameter
    data class Gradle(val version: String) : DokkaGradlePluginTestParameter
    data class KotlinGradlePlugin(val version: String) : DokkaGradlePluginTestParameter
}

data class DokkaGradlePluginTestParameters(
    val agpVersion: String,
    val gradleVersion: String,
    val kgpVersion: String,
    val analysisMode: String,
) {

    fun isValid() {

    }

}

// junit.jupiter.tempdir.cleanup.mode.default

fun a() {
    val agpVersions = Exhaustive.of("")
    val gradleVersions = Exhaustive.of("")
    val kgpVersions = Exhaustive.of("")
    val analysisMode = Exhaustive.of("")

    val params = Arb.bind(
        agpVersions,
        gradleVersions,
        kgpVersions,
        analysisMode,
    ) { agp, gradle, kgp, analysis ->
        DokkaGradlePluginTestParameters(
            agpVersion = agp,
            gradleVersion = gradle,
            kgpVersion = kgp,
            analysisMode = analysis,
        )
    }
}

class FooArgumentsProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
        return Stream.of(
            Arguments.of(Foo("1.0")),
            Arguments.of(Foo("2.0")),
            Arguments.of(Foo("3.0"))
        )
    }
}

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(CustomFooTestExtension::class)
annotation class CustomFooTest(val parameter: FooParameter = FooParameter.DEFAULT)

enum class FooParameter {
    DEFAULT
}

class CustomFooTestExtension : ParameterResolver, TestInstancePostProcessor, BeforeEachCallback {

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return parameterContext.parameter.type == Foo::class.java
    }

    override fun resolveParameter(parameterContext: ParameterContext, context: ExtensionContext): Any {
        val foo = Foo("1.0") // Example; replace with actual dynamic logic as needed
        context.getStore(ExtensionContext.Namespace.GLOBAL).put("foo", foo)
        return foo
    }

//    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
//        val foo = Foo("1.0") // Example; replace with actual dynamic logic as needed
//        extensionContext.testInstance.ifPresent { instance ->
//            val tags = mutableSetOf<String>()
//            if (foo.version == "1.0") {
//                tags.add("version-1")
//            }
//            val testDescriptor = extensionContext
//               .root
//               .uniqueId
//               .resolve(AnnotationSupport.findAnnotation(instance.javaClass, CustomFooTest::class.java)
//               .get())
//               .root
//            testDescriptor.addTags(*(tags.toTypedArray()))
//        }
//        return foo
//    }

    override fun beforeEach(context: ExtensionContext) {
        val foo = context.getStore(ExtensionContext.Namespace.GLOBAL).get("foo", Foo::class.java) ?: return
        if (foo.version == "1.0") {
            context.testInstance.ifPresent { instance ->
                instance.javaClass.declaredMethods.forEach { method ->
                    if (method.isAnnotationPresent(CustomFooTest::class.java)) {
                        method.annotations.filterIsInstance<Tag>().forEach {
                            context.tags.add(it.value)
                        }
                    }
                }
            }
        }
    }

    override fun postProcessTestInstance(testInstance: Any, context: ExtensionContext) {
        // Implement any additional instance post-processing logic if needed
    }
}


data class SemVer(
    val major: Int,
    val minor: Int,
    val patch: Int,
) : Comparable<SemVer> {
    override fun compareTo(other: SemVer): Int {
        return when {
            major != other.major -> major.compareTo(other.major)
            minor != other.minor -> minor.compareTo(other.minor)
            patch != other.patch -> patch.compareTo(other.patch)
            else -> 0
        }
    }
}

fun SemVer(version: String): SemVer {
    val split = version
        .split('.')
        .map { it.toInt() }

    require(split.size == 3) { "Version '$version' is not SemVer" }
    val (major, minor, patch) = split
    return SemVer(major, minor, patch)
}
