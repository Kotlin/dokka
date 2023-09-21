/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test.api.jvm.mixed

import org.jetbrains.dokka.analysis.test.api.configuration.BaseTestDokkaConfigurationBuilder
import org.jetbrains.dokka.analysis.test.api.configuration.TestDokkaConfiguration
import org.jetbrains.dokka.analysis.test.api.jvm.java.JavaTestSourceSetBuilder
import org.jetbrains.dokka.analysis.test.api.jvm.kotlin.KotlinJvmTestSourceSetBuilder
import org.jetbrains.dokka.analysis.test.api.util.AnalysisTestDslMarker

/**
 * An implementation of [BaseTestDokkaConfigurationBuilder] specific to [MixedJvmTestProject].
 */
class MixedJvmTestConfigurationBuilder : BaseTestDokkaConfigurationBuilder() {
    override var moduleName: String = "mixedJvmTestProject"

    private val kotlinSourceSetBuilder = KotlinJvmTestSourceSetBuilder()
    private val javaSourceSetBuilder = JavaTestSourceSetBuilder()

    @AnalysisTestDslMarker
    fun kotlinSourceSet(fillSourceSet: KotlinJvmTestSourceSetBuilder.() -> Unit) {
        fillSourceSet(kotlinSourceSetBuilder)
    }

    @AnalysisTestDslMarker
    fun javaSourceSet(fillSourceSet: JavaTestSourceSetBuilder.() -> Unit) {
        fillSourceSet(javaSourceSetBuilder)
    }

    override fun verify() {
        super.verify()
        kotlinSourceSetBuilder.verify()
        javaSourceSetBuilder.verify()
    }

    override fun build(): TestDokkaConfiguration {
        return TestDokkaConfiguration(
            moduleName = moduleName,
            includes = includes,
            sourceSets = setOf(
                kotlinSourceSetBuilder.build(),
                javaSourceSetBuilder.build()
            )
        )
    }

    override fun toString(): String {
        return "MixedJvmTestConfigurationBuilder(" +
                "moduleName='$moduleName', " +
                "kotlinSourceSetBuilder=$kotlinSourceSetBuilder, " +
                "javaSourceSetBuilder=$javaSourceSetBuilder" +
                ")"
    }
}
