/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test.jvm.mixed

import org.jetbrains.dokka.analysis.test.api.mixedJvmTestProject
import org.jetbrains.dokka.analysis.test.api.parse
import kotlin.test.Test
import kotlin.test.assertEquals

class MixedJvmAnalysisTest {

    @Test
    fun mixedJvmTestProject() {
        val testProject = mixedJvmTestProject {
            kotlinSourceSet {
                ktFile(pathFromSrc = "test/MyFile.kt") {
                    +"fun foo(): String = \"Foo\""
                }
                javaFile(pathFromSrc = "test/MyJavaFileInKotlin.java") {
                    +"""
                        public class MyJavaFileInKotlin {
                            public static void bar() {
                                System.out.println("Bar");
                            }
                        }
                    """
                }
            }

            javaSourceSet {
                ktFile(pathFromSrc = "test/MyFile.kt") {
                    +"fun bar(): String = \"Foo\""
                }
                javaFile(pathFromSrc = "test/MyJavaFileInJava.java") {
                    +"""
                        public class MyJavaFileInJava {
                            public static void bar() {
                                System.out.println("Bar");
                            }
                        }
                    """
                }
            }
        }

        val module = testProject.parse()
        assertEquals(1, module.packages.size)

        val pckg = module.packages[0]
        assertEquals("test", pckg.name)

        assertEquals(2, pckg.classlikes.size)
        assertEquals(2, pckg.functions.size)

        val firstClasslike = pckg.classlikes[0]
        assertEquals("MyJavaFileInKotlin", firstClasslike.name)

        val secondClasslike = pckg.classlikes[1]
        assertEquals("MyJavaFileInJava", secondClasslike.name)

        val firstFunction = pckg.functions[0]
        assertEquals("foo", firstFunction.name)

        val secondFunction = pckg.functions[1]
        assertEquals("bar", secondFunction.name)
    }
}
