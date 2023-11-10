/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.javadoc

import org.jetbrains.dokka.javadoc.pages.JavadocClasslikePageNode
import kotlin.test.Test
import kotlin.test.assertEquals

internal class JavadocLocationTemplateMapTest : AbstractJavadocTemplateMapTest() {
    @Test
    fun `should have correct location to root from class`(){
        dualTestTemplateMapInline(
            kotlin =
            """
            /src/source0.kt
            package com.test.package0
            /**
            * Documentation for TestClass
            */
            class TestClass
            """,
            java =
            """
            /src/com/test/package0/TestClass.java
            package com.test.package0;
            /**
            * Documentation for TestClass
            */
            public final class TestClass {}
            """
        ) {
            val map = singlePageOfType<JavadocClasslikePageNode>().templateMap
            assertEquals("TestClass", map["name"])
            assertEquals("TestClass", map["title"])
            //This is taken from the expected location of files based on the package, so:
            //com -> test -> package0
            assertEquals("../../../", map["pathToRoot"])
        }
    }
}
