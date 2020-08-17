package utilities

import org.jetbrains.dokka.utilities.SelfRepresentingSingletonSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SelfRepresentingSingletonSetTest {

    data class TestModel(val index: Int = 0) : SelfRepresentingSingletonSet<TestModel>

    @Test
    fun size() {
        assertEquals(1, TestModel().size)
    }

    @Test
    fun contains() {
        val m0 = TestModel(0)
        val m1 = TestModel(1)

        assertFalse(m1 in m0)
        assertFalse(m0 in m1)
        assertTrue(m0 in m0)
        assertTrue(m1 in m1)
        assertTrue(TestModel(0) in m0)
    }

    @Test
    fun `containsAll is compliant to setOf`() {
        val setOf = setOf(TestModel())
        val testModel = TestModel()

        assertEquals(
            setOf.containsAll(emptyList()), testModel.containsAll(emptyList())
        )

        assertEquals(
            setOf.containsAll(listOf(TestModel())), testModel.containsAll(listOf(TestModel()))
        )

        assertEquals(
            setOf.containsAll(listOf(TestModel(0), TestModel(1))),
            testModel.containsAll(listOf(TestModel(0), TestModel(1)))
        )
    }

    @Test
    fun isEmpty() {
        assertFalse(TestModel().isEmpty())
    }

    @Test
    fun iterator() {
        assertEquals(
            listOf(TestModel()), TestModel(0).iterator().asSequence().toList()
        )
    }
}
