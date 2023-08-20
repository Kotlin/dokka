package translators

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.links.DRI
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import utils.JavaCode

class Bug1341 : BaseAbstractTest() {
    @JavaCode
    @Test
    fun `reproduce bug #1341`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src")
                    analysisPlatform = "jvm"
                }
            }
        }

        testInline(
            """
            /src/com/sample/OtherClass.kt
            package com.sample
            class OtherClass internal constructor() {
                internal annotation class CustomAnnotation
            }
            
            /src/com/sample/ClassUsingAnnotation.java
            package com.sample
            public class ClassUsingAnnotation {
                @OtherClass.CustomAnnotation
                public int doSomething() {
                    return 1;
                }
            }
            """.trimIndent(),
            configuration
        ) {
            this.documentablesMergingStage = { module ->
                assertEquals(DRI("com.sample"), module.packages.single().dri)
            }
        }
    }
}
