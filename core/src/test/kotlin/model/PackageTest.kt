package org.jetbrains.dokka.tests

import org.jetbrains.dokka.*
import org.jetbrains.kotlin.cli.common.config.KotlinSourceRoot
import org.junit.Assert.*
import org.junit.Test

abstract class BasePackageTest(val analysisPlatform: Platform) {
    val defaultModelConfig = ModelConfig(analysisPlatform = analysisPlatform)
    @Test fun rootPackage() {
        checkSourceExistsAndVerifyModel("testdata/packages/rootPackage.kt", defaultModelConfig) { model ->
            with(model.members.single()) {
                assertEquals(NodeKind.Package, kind)
                assertEquals("", name)
                assertEquals(Content.Empty, content)
                assertTrue(details.none())
                assertTrue(members.none())
                assertTrue(links.none())
            }
        }
    }

    @Test fun simpleNamePackage() {
        checkSourceExistsAndVerifyModel("testdata/packages/simpleNamePackage.kt", defaultModelConfig) { model ->
            with(model.members.single()) {
                assertEquals(NodeKind.Package, kind)
                assertEquals("simple", name)
                assertEquals(Content.Empty, content)
                assertTrue(details.none())
                assertTrue(members.none())
                assertTrue(links.none())
            }
        }
    }

    @Test fun dottedNamePackage() {
        checkSourceExistsAndVerifyModel("testdata/packages/dottedNamePackage.kt", defaultModelConfig) { model ->
            with(model.members.single()) {
                assertEquals(NodeKind.Package, kind)
                assertEquals("dot.name", name)
                assertEquals(Content.Empty, content)
                assertTrue(details.none())
                assertTrue(members.none())
                assertTrue(links.none())
            }
        }
    }

    @Test fun multipleFiles() {
        verifyModel(
            ModelConfig(
                roots = arrayOf(
                    KotlinSourceRoot("testdata/packages/dottedNamePackage.kt", false),
                    KotlinSourceRoot("testdata/packages/simpleNamePackage.kt", false)
                ),
                analysisPlatform = analysisPlatform
            )
        ) { model ->
            assertEquals(2, model.members.count())
            with(model.members.single { it.name == "simple" }) {
                assertEquals(NodeKind.Package, kind)
                assertEquals("simple", name)
                assertEquals(Content.Empty, content)
                assertTrue(details.none())
                assertTrue(members.none())
                assertTrue(links.none())
            }
            with(model.members.single { it.name == "dot.name" }) {
                assertEquals(NodeKind.Package, kind)
                assertEquals(Content.Empty, content)
                assertTrue(details.none())
                assertTrue(members.none())
                assertTrue(links.none())
            }
        }
    }

    @Test fun multipleFilesSamePackage() {
        verifyModel(
            ModelConfig(
                roots = arrayOf(
                    KotlinSourceRoot("testdata/packages/simpleNamePackage.kt", false),
                    KotlinSourceRoot("testdata/packages/simpleNamePackage2.kt", false)
                ),
                analysisPlatform = analysisPlatform
            )
        ) { model ->
            assertEquals(1, model.members.count())
            with(model.members.elementAt(0)) {
                assertEquals(NodeKind.Package, kind)
                assertEquals("simple", name)
                assertEquals(Content.Empty, content)
                assertTrue(details.none())
                assertTrue(members.none())
                assertTrue(links.none())
            }
        }
    }

    @Test fun classAtPackageLevel() {
        verifyModel(
            ModelConfig(
                roots = arrayOf(KotlinSourceRoot("testdata/packages/classInPackage.kt", false)),
                analysisPlatform = analysisPlatform
            )
        ) { model ->
            assertEquals(1, model.members.count())
            with(model.members.elementAt(0)) {
                assertEquals(NodeKind.Package, kind)
                assertEquals("simple.name", name)
                assertEquals(Content.Empty, content)
                assertTrue(details.none())
                assertEquals(1, members.size)
                assertTrue(links.none())
            }
        }
    }

    @Test fun suppressAtPackageLevel() {
        verifyModel(
            ModelConfig(
                roots = arrayOf(KotlinSourceRoot("testdata/packages/classInPackage.kt", false)),
                perPackageOptions = listOf(
                    PackageOptionsImpl(prefix = "simple.name", suppress = true)
                ),
                analysisPlatform = analysisPlatform
            )
        ) { model ->
            assertEquals(0, model.members.count())
        }
    }
}

class JSPackageTest : BasePackageTest(Platform.js)
class JVMPackageTest : BasePackageTest(Platform.jvm)
class CommonPackageTest : BasePackageTest(Platform.common)