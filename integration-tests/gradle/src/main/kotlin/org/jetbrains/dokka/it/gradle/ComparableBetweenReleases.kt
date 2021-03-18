package org.jetbrains.dokka.it.gradle

import org.jetbrains.dokka.it.AbstractIntegrationTest
import org.junit.rules.TemporaryFolder

abstract class ComparableBetweenReleases(val buildVersions: BuildVersions) {
    val versions: List<String>
        get() = listOf("1.4.30", "for-integration-tests-SNAPSHOT")


}