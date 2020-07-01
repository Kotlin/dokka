package org.jetbrains.dokka.it.gradle

import org.junit.Test

abstract class AbstractDefaultVersionsGradleIntegrationTest : AbstractGradleIntegrationTest() {

    protected abstract fun execute(versions: BuildVersions)

    @Test
    fun `gradle 5_6_4 kotlin 1_3_72`() {
        execute(
            BuildVersions(
                gradleVersion = "5.6.4",
                kotlinVersion = "1.3.72"
            )
        )
    }

    @Test
    fun `gradle 5_6_4 kotlin 1_3_30`() {
        execute(
            BuildVersions(
                gradleVersion = "5.6.4",
                kotlinVersion = "1.3.30"
            )
        )
    }

    @Test
    fun `gradle 5_6_4 kotlin 1_4_M2_eap_70`() {
        execute(
            BuildVersions(
                gradleVersion = "5.6.4",
                kotlinVersion = "1.4-M2-eap-70"
            )
        )
    }

    @Test
    fun `gradle 6_1_1 kotlin 1_3_72`() {
        execute(
            BuildVersions(
                gradleVersion = "6.1.1",
                kotlinVersion = "1.3.72"
            )
        )
    }

    @Test
    fun `gradle 6_5_1 kotlin 1_4_M2_eap_70`() {
        execute(
            BuildVersions(
                gradleVersion = "6.5.1",
                kotlinVersion = "1.4-M2-eap-70"
            )
        )
    }

    @Test
    fun `gradle 6_5_1 kotlin 1_3_72`() {
        execute(
            BuildVersions(
                gradleVersion = "6.5.1",
                kotlinVersion = "1.3.72"
            )
        )
    }
}
