package org.jetbrains.dokka.tests

import junit.framework.TestCase.assertEquals
import org.jetbrains.dokka.Content
import org.jetbrains.dokka.NodeKind
import org.junit.Test

class TypeAliasTest {
    @Test
    fun testSimple() {
        checkSourceExistsAndVerifyModel("testdata/typealias/simple.kt") {
            val pkg = it.members.single()
            with(pkg.member(NodeKind.TypeAlias)) {
                assertEquals(Content.Empty, content)
                assertEquals("B", name)
                assertEquals("A", detail(NodeKind.TypeAliasUnderlyingType).name)
            }
        }
    }

    @Test
    fun testInheritanceFromTypeAlias() {
        checkSourceExistsAndVerifyModel("testdata/typealias/inheritanceFromTypeAlias.kt") {
            val pkg = it.members.single()
            with(pkg.member(NodeKind.TypeAlias)) {
                assertEquals(Content.Empty, content)
                assertEquals("Same", name)
                assertEquals("Some", detail(NodeKind.TypeAliasUnderlyingType).name)
                assertEquals("My", inheritors.single().name)
            }
            with(pkg.members(NodeKind.Class).find { it.name == "My" }!!) {
                assertEquals("Same", detail(NodeKind.Supertype).name)
            }
        }
    }

    @Test
    fun testChain() {
        checkSourceExistsAndVerifyModel("testdata/typealias/chain.kt") {
            val pkg = it.members.single()
            with(pkg.members(NodeKind.TypeAlias).find { it.name == "B" }!!) {
                assertEquals(Content.Empty, content)
                assertEquals("A", detail(NodeKind.TypeAliasUnderlyingType).name)
            }
            with(pkg.members(NodeKind.TypeAlias).find { it.name == "C" }!!) {
                assertEquals(Content.Empty, content)
                assertEquals("B", detail(NodeKind.TypeAliasUnderlyingType).name)
            }
        }
    }

    @Test
    fun testDocumented() {
        checkSourceExistsAndVerifyModel("testdata/typealias/documented.kt") {
            val pkg = it.members.single()
            with(pkg.member(NodeKind.TypeAlias)) {
                assertEquals("Just typealias", content.summary.toTestString())
            }
        }
    }

    @Test
    fun testDeprecated() {
        checkSourceExistsAndVerifyModel("testdata/typealias/deprecated.kt") {
            val pkg = it.members.single()
            with(pkg.member(NodeKind.TypeAlias)) {
                assertEquals(Content.Empty, content)
                assertEquals("Deprecated", deprecation!!.name)
                assertEquals("\"Not mainstream now\"", deprecation!!.detail(NodeKind.Parameter).detail(NodeKind.Value).name)
            }
        }
    }

    @Test
    fun testGeneric() {
        checkSourceExistsAndVerifyModel("testdata/typealias/generic.kt") {
            val pkg = it.members.single()
            with(pkg.members(NodeKind.TypeAlias).find { it.name == "B" }!!) {
                assertEquals("Any", detail(NodeKind.TypeAliasUnderlyingType).detail(NodeKind.Type).name)
            }

            with(pkg.members(NodeKind.TypeAlias).find { it.name == "C" }!!) {
                assertEquals("T", detail(NodeKind.TypeAliasUnderlyingType).detail(NodeKind.Type).name)
                assertEquals("T", detail(NodeKind.TypeParameter).name)
            }
        }
    }

    @Test
    fun testFunctional() {
        checkSourceExistsAndVerifyModel("testdata/typealias/functional.kt") {
            val pkg = it.members.single()
            with(pkg.member(NodeKind.TypeAlias)) {
                assertEquals("Function1", detail(NodeKind.TypeAliasUnderlyingType).name)
                val typeParams = detail(NodeKind.TypeAliasUnderlyingType).details(NodeKind.Type)
                assertEquals("A", typeParams.first().name)
                assertEquals("B", typeParams.last().name)
            }

            with(pkg.member(NodeKind.Function)) {
                assertEquals("Spell", detail(NodeKind.Parameter).detail(NodeKind.Type).name)
            }
        }
    }

    @Test
    fun testAsTypeBoundWithVariance() {
        checkSourceExistsAndVerifyModel("testdata/typealias/asTypeBoundWithVariance.kt") {
            val pkg = it.members.single()
            with(pkg.members(NodeKind.Class).find { it.name == "C" }!!) {
                val tParam = detail(NodeKind.TypeParameter)
                assertEquals("out", tParam.detail(NodeKind.Modifier).name)
                assertEquals("B", tParam.detail(NodeKind.Type).link(NodeKind.TypeAlias).name)
            }

            with(pkg.members(NodeKind.Class).find { it.name == "D" }!!) {
                val tParam = detail(NodeKind.TypeParameter)
                assertEquals("in", tParam.detail(NodeKind.Modifier).name)
                assertEquals("B", tParam.detail(NodeKind.Type).link(NodeKind.TypeAlias).name)
            }
        }
    }

    @Test
    fun sinceKotlin() {
        checkSourceExistsAndVerifyModel("testdata/typealias/sinceKotlin.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("1.1", sinceKotlin)
            }
        }
    }
}