/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test.api.jvm.mixed

import org.jetbrains.dokka.analysis.test.api.TestData
import org.jetbrains.dokka.analysis.test.api.TestDataFile
import org.jetbrains.dokka.analysis.test.api.TestProject
import org.jetbrains.dokka.analysis.test.api.configuration.TestDokkaConfiguration
import org.jetbrains.dokka.analysis.test.api.jvm.java.JavaTestProject
import org.jetbrains.dokka.analysis.test.api.jvm.kotlin.KotlinJvmTestProject
import org.jetbrains.dokka.analysis.test.api.markdown.MarkdownTestData
import org.jetbrains.dokka.analysis.test.api.markdown.MarkdownTestDataFile
import org.jetbrains.dokka.analysis.test.api.markdown.MdFileCreator
import org.jetbrains.dokka.analysis.test.api.mixedJvmTestProject
import org.jetbrains.dokka.analysis.test.api.util.AnalysisTestDslMarker
import org.jetbrains.dokka.analysis.test.api.util.flatListOf

/**
 * @see mixedJvmTestProject for an explanation and a convenient way to construct this project
 */
class MixedJvmTestProject : TestProject, MdFileCreator {

    private val projectConfigurationBuilder = MixedJvmTestConfigurationBuilder()

    private val kotlinSourceSet = MixedJvmTestData(pathToSources = KotlinJvmTestProject.DEFAULT_SOURCE_ROOT)
    private val javaSourceSet = MixedJvmTestData(pathToSources = JavaTestProject.DEFAULT_SOURCE_ROOT)
    private val markdownTestData = MarkdownTestData()

    @AnalysisTestDslMarker
    fun dokkaConfiguration(fillConfiguration: MixedJvmTestConfigurationBuilder.() -> Unit) {
        fillConfiguration(projectConfigurationBuilder)
    }

    @AnalysisTestDslMarker
    fun kotlinSourceSet(fillTestData: MixedJvmTestData.() -> Unit) {
        fillTestData(kotlinSourceSet)
    }

    @AnalysisTestDslMarker
    fun javaSourceSet(fillTestData: MixedJvmTestData.() -> Unit) {
        fillTestData(javaSourceSet)
    }

    @AnalysisTestDslMarker
    override fun mdFile(pathFromProjectRoot: String, fillFile: MarkdownTestDataFile.() -> Unit) {
        markdownTestData.mdFile(pathFromProjectRoot, fillFile)
    }

    override fun verify() {
        projectConfigurationBuilder.verify()
    }

    override fun getConfiguration(): TestDokkaConfiguration {
        return projectConfigurationBuilder.build()
    }

    override fun getTestData(): TestData {
        return object : TestData {
            override fun getFiles(): List<TestDataFile> {
                return flatListOf(
                    this@MixedJvmTestProject.kotlinSourceSet.getFiles(),
                    this@MixedJvmTestProject.javaSourceSet.getFiles(),
                    this@MixedJvmTestProject.markdownTestData.getFiles()
                )
            }
        }
    }

    override fun toString(): String {
        return "MixedJvmTestProject(" +
                "projectConfigurationBuilder=$projectConfigurationBuilder, " +
                "kotlinSourceSet=$kotlinSourceSet, " +
                "javaSourceSet=$javaSourceSet, " +
                "markdownTestData=$markdownTestData" +
                ")"
    }
}

