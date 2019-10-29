package org.jetbrains.dokka.tests.dri

import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.resolvers.toJavadocLocation
import org.junit.Test
import kotlin.test.assertEquals

class DRITest {
    @Test
    fun onlyClassNames() {
        val expected = DRI(classNames = "className1.className2")
        val actual = DRI.from("/className1.className2////")
        assertEquals(expected, actual)
    }

    @Test
    fun fullDRI() {
        val expected = DRI("org.dokka", "className1.className2", Callable("<init>", "", "", listOf("Int")), 2, "something" )
        val actual = DRI.from("org.dokka/className1.className2/<init>/..Int/2/something")
        assertEquals(expected, actual)
    }

    @Test
    fun onlyExtra() {
        val expected = DRI(null, null, null, null, "extra" )
        val actual = DRI.from("/////extra")
        assertEquals(expected, actual)
    }

    @Test
    fun javadoc8Location() {
        val dri = DRI("org.jetbrains.dokka", "DRITest", "javadocLocation", ".void.")
        assertEquals("org/jetbrains/dokka/DRITest.html#javadocLocation--", dri.toJavadocLocation(8))
    }
}

