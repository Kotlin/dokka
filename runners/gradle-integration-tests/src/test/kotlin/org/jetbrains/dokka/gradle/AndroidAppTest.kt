package org.jetbrains.dokka.gradle

import org.junit.Test

class AndroidAppTest : AbstractAndroidAppTest("androidApp") {
    @Test
    fun `test kotlin 1_1_2-5 and gradle 4_0 and abt 3_0_0-alpha3`() {
        doTest("4.0", "1.1.2-5", AndroidPluginParams("3.0.0-alpha3", "25.0.2", 25))
    }

    @Test
    fun `test kotlin 1_1_2 and gradle 3_5 and abt 2_3_0`() {
        doTest("3.5", "1.1.2", AndroidPluginParams("2.3.0", "25.0.0", 24))
    }

    @Test
    fun `test kotlin 1_0_7 and gradle 2_14_1 and abt 2_2_3`() {
        doTest("2.14.1", "1.0.7", AndroidPluginParams("2.2.3", "25.0.0", 24))
    }

    @Test
    fun `test kotlin 1_2_20 and gradle 4_5 and abt 3_0_1`() {
        doTest("4.5", "1.2.20", AndroidPluginParams("3.0.1", "27.0.0", 27))
    }
}