package org.jetbrains.dokka.tests.externalLocationProviders

import junit.framework.Assert.assertEquals
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.resolvers.ExternalLocationProvider
import org.junit.Test


class ExternalLocationProviderTest {
    val stdlibLink = DokkaConfiguration.ExternalDocumentationLink.Builder("https://kotlinlang.org/api/latest/jvm/stdlib/").build()
    @Test fun kotlinString() {
        val dri = DRI("kotlin", "String")
        val link = ExternalLocationProvider.getLocation(dri, listOf(stdlibLink.packageListUrl))
        assertEquals("kotlin/-string/index.html", link)
    }

    @Test fun kotlinCompareTo() {
        val dri = DRI("kotlin", "String", "compareTo", "#Int#String")
        val link = ExternalLocationProvider.getLocation(dri, listOf(stdlibLink.packageListUrl))
        assertEquals("kotlin/-string/compare-to.html", link)
    }

}

