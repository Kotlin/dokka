package model

import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.model.CompositeSourceSetID
import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.model.sourceSetIDs
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DisplaySourceSetTest {
    @Test
    fun `contains sourceSetId`() {
        val contentSourceSet = DisplaySourceSet(
            sourceSetIDs = CompositeSourceSetID(listOf(DokkaSourceSetID("m1", "s1"), DokkaSourceSetID("m2", "s2"))),
            name = "displayName",
            platform = Platform.common
        )

        assertFalse(
            DokkaSourceSetID("m3", "s3") in contentSourceSet.sourceSetIDs,
            "Expected source set id not being contained in content source set"
        )

        assertTrue(
            DokkaSourceSetID("m1", "s1") in contentSourceSet.sourceSetIDs,
            "Expected source set id being contained in content source set"
        )

        assertTrue(
            DokkaSourceSetID("m1+m2", "s1+s2") in contentSourceSet.sourceSetIDs,
            "Expected merged source set being contained in content source set"
        )
    }

    @Test
    fun `Iterable contains sourceSetId`() {

        val contentSourceSet = DisplaySourceSet(
            sourceSetIDs = CompositeSourceSetID(listOf(DokkaSourceSetID("m1", "s1"), DokkaSourceSetID("m2", "s2"))),
            name = "displayName",
            platform = Platform.common
        )

        assertFalse(
            DokkaSourceSetID("m3", "s3") in listOf(contentSourceSet).sourceSetIDs,
            "Expected source set id not being contained in content source set"
        )

        assertTrue(
            DokkaSourceSetID("m1", "s1") in listOf(contentSourceSet).sourceSetIDs,
            "Expected source set id being contained in content source set"
        )

        assertTrue(
            DokkaSourceSetID("m1+m2", "s1+s2") in listOf(contentSourceSet).sourceSetIDs,
            "Expected merged source set being contained in content source set"
        )
    }
}
