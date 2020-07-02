package org.jetbrains.dokka.it.gradle

import org.gradle.util.GradleVersion
import org.junit.Assume
import org.junit.Test

abstract class AbstractDefaultVersionsGradleIntegrationTest(
    private val minGradleVersion: GradleVersion? = null,
    private val maxGradleVersion: GradleVersion? = null
) : AbstractGradleIntegrationTest() {

    protected abstract fun execute(versions: BuildVersions)

    private fun executeIfRequirementsAreMet(versions: BuildVersions) {
        if (minGradleVersion != null) {
            Assume.assumeTrue(versions.gradleVersion >= minGradleVersion)
        }
        if (maxGradleVersion != null) {
            Assume.assumeTrue(versions.gradleVersion <= maxGradleVersion)
        }

        execute(versions )
    }

    @Test
    open fun `gradle 5_6_4 kotlin 1_3_72`() {
        executeIfRequirementsAreMet(
            BuildVersions(
                gradleVersion = "5.6.4",
                kotlinVersion = "1.3.72"
            )
        )
    }

    @Test
    open fun `gradle 5_6_4 kotlin 1_3_30`() {
        executeIfRequirementsAreMet(
            BuildVersions(
                gradleVersion = "5.6.4",
                kotlinVersion = "1.3.30"
            )
        )
    }

    @Test
    open fun `gradle 5_6_4 kotlin 1_4_M2_eap_70`() {
        executeIfRequirementsAreMet(
            BuildVersions(
                gradleVersion = "5.6.4",
                kotlinVersion = "1.4-M2-eap-70"
            )
        )
    }

    @Test
    open fun `gradle 6_1_1 kotlin 1_3_72`() {
        executeIfRequirementsAreMet(
            BuildVersions(
                gradleVersion = "6.1.1",
                kotlinVersion = "1.3.72"
            )
        )
    }

    @Test
    open fun `gradle 6_5_1 kotlin 1_4_M2_eap_70`() {
        executeIfRequirementsAreMet(
            BuildVersions(
                gradleVersion = "6.5.1",
                kotlinVersion = "1.4-M2-eap-70"
            )
        )
    }

    @Test
    open fun `gradle 6_5_1 kotlin 1_3_72`() {
        executeIfRequirementsAreMet(
            BuildVersions(
                gradleVersion = "6.5.1",
                kotlinVersion = "1.3.72"
            )
        )
    }
}
