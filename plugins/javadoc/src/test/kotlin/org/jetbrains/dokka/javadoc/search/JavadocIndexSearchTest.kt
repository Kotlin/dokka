package org.jetbrains.dokka.javadoc.search

import org.jetbrains.dokka.javadoc.AbstractJavadocTemplateMapTest
import org.junit.jupiter.api.Test
import utils.TestOutputWriterPlugin
import org.junit.jupiter.api.Assertions.*

internal class JavadocIndexSearchTest : AbstractJavadocTemplateMapTest() {
    @Test
    fun `javadoc index search tag`(){
        val writerPlugin = TestOutputWriterPlugin()
        dualTestTemplateMapInline(
            java = """
            /src/ClassA.java
            package package0
            /** 
            * Documentation for ClassA 
            * Defines the implementation of the system Java compiler and its command line equivalent, {@index javac}, as well as javah.
            */
            class ClassA {
            
            }
            """,
            pluginsOverride = listOf(writerPlugin)
        ) {
            val contents = writerPlugin.writer.contents
            val expectedSearchTagJson = """var tagSearchIndex = [{"p":"package0","c":"ClassA","l":"javac","url":"package0/ClassA.html#javac"}]"""
            assertEquals(expectedSearchTagJson, contents["tag-search-index.js"])
        }
    }

    @Test
    fun `javadoc type with member search`(){
        val writerPlugin = TestOutputWriterPlugin()
        dualTestTemplateMapInline(
            java = """
            /src/ClassA.java
            package package0
            class ClassA {
                public String propertyOfClassA = "Sample";
                
                public void sampleFunction(){
                    
                }
            }
            """,
            pluginsOverride = listOf(writerPlugin)
        ) {
            val contents = writerPlugin.writer.contents
            val expectedPackageJson = """var packageSearchIndex = [{"l":"package0","url":"package0/package-summary.html"}, {"l":"All packages","url":"index.html"}]"""
            val expectedClassesJson = """var typeSearchIndex = [{"p":"package0","l":"ClassA","url":"package0/ClassA.html"}, {"l":"All classes","url":"allclasses.html"}]"""
            val expectedMembersJson = """var memberSearchIndex = [{"p":"package0","c":"ClassA","l":"sampleFunction()","url":"package0/ClassA.html#sampleFunction()"}, {"p":"package0","c":"ClassA","l":"propertyOfClassA","url":"package0/ClassA.html#propertyOfClassA"}]"""

            assertEquals(expectedPackageJson, contents["package-search-index.js"])
            assertEquals(expectedClassesJson, contents["type-search-index.js"])
            assertEquals(expectedMembersJson, contents["member-search-index.js"])
        }
    }
}