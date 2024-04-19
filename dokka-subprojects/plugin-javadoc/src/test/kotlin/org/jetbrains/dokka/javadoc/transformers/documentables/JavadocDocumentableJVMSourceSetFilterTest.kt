/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.javadoc.transformers.documentables

import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import kotlin.test.Test
import kotlin.test.assertEquals

class JavadocDocumentableJVMSourceSetFilterTest : BaseAbstractTest() {

    private val config = dokkaConfiguration {
        format = "javadoc"
        sourceSets {
            sourceSet {
                sourceRoots = listOf("jvmSrc/")
                analysisPlatform = "jvm"
                name = "jvm"
                dependentSourceSets = setOf(DokkaSourceSetID("root", "common"))
            }
            sourceSet {
                sourceRoots = listOf("commonSrc/")
                analysisPlatform = "common"
                name = "common"
            }
            sourceSet {
                sourceRoots = listOf("otherCommonSrc/")
                analysisPlatform = "common"
                name = "otherCommon"
            }
        }
    }
    private val query = """
            /jvmSrc/source0.kt
            package package0
            /** 
            * Documentation for ClassA 
            */
            class ClassA {
                fun a() {}
                fun b() {}
                fun c() {}
            }
            
            /jsSrc/source1.kt
            package package1
            /**
            * Documentation for ClassB
            */
            class ClassB {
                fun d() {}
                fun e() {}
                fun f() {}
            }
            
            /commonSrc/source2.kt
            package package1
            /**
            * Documentation for ClassC
            */
            class ClassC {
                fun g() {}
                fun h() {}
                fun j() {}
            }
            
            /otherCommonSrc/source3.kt
            package package1
            /**
            * Documentation for ClassD
            */
            class ClassD {
                fun l() {}
                fun m() {}
                fun n() {}
            }
        """.trimIndent()

    @Test
    fun `non-jvm and not dependent common source sets are ommited`() {
        testInline(query, config) {
            preMergeDocumentablesTransformationStage = { modules ->
                assertEquals(2, modules.size)
                modules.forEach { assertEquals(1, it.sourceSets.size) }
                assertEquals(
                    setOf(
                        // otherCommon is not included
                        DokkaSourceSetID("root", "common"),
                        DokkaSourceSetID("root", "jvm"),
                    ),
                    modules.flatMap { it.sourceSets }.map { it.sourceSetID }.toSet()
                )
            }
        }
    }
}
