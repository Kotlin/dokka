package model

import org.jetbrains.dokka.model.DPackage
import org.junit.jupiter.api.Test
import utils.AbstractModelTest

class PackagesTest : AbstractModelTest("/src/main/kotlin/packages/Test.kt", "packages") {

    @Test
    fun rootPackage() {
        inlineModelTest(
            """
                |
            """.trimIndent(),
            prependPackage = false
        ) {
            with((this / "[JVM root]").cast<DPackage>()) {
                name equals "[JVM root]"
                children counts 0
            }
        }
    }

    @Test
    fun simpleNamePackage() {
        inlineModelTest(
            """
                |package simple
            """.trimIndent(),
            prependPackage = false
        ) {
            with((this / "simple").cast<DPackage>()) {
                name equals "simple"
                children counts 0
            }
        }
    }

    @Test
    fun dottedNamePackage() {
        inlineModelTest(
            """
                |package dot.name
            """.trimIndent(),
            prependPackage = false
        ) {
            with((this / "dot.name").cast<DPackage>()) {
                name equals "dot.name"
                children counts 0
            }
        }

    }

    @Test
    fun multipleFiles() {
        inlineModelTest(
            """
                |package dot.name
                |/src/main/kotlin/packages/Test2.kt
                |package simple
            """.trimIndent(),
            prependPackage = false
        ) {
            children counts 2
            with((this / "dot.name").cast<DPackage>()) {
                name equals "dot.name"
                children counts 0
            }
            with((this / "simple").cast<DPackage>()) {
                name equals "simple"
                children counts 0
            }
        }
    }

    @Test
    fun multipleFilesSamePackage() {
        inlineModelTest(
            """
                |package simple
                |/src/main/kotlin/packages/Test2.kt
                |package simple
            """.trimIndent(),
            prependPackage = false
        ) {
            children counts 1
            with((this / "simple").cast<DPackage>()) {
                name equals "simple"
                children counts 0
            }
        }
    }

    @Test
    fun classAtPackageLevel() {
        inlineModelTest(
            """
                |package simple.name
                |
                |class Foo {}
            """.trimIndent(),
            prependPackage = false
        ) {
            with((this / "simple.name").cast<DPackage>()) {
                name equals "simple.name"
                children counts 1
            }
        }
    }

    // todo
//    @Test fun suppressAtPackageLevel() {
//        verifyModel(
//            ModelConfig(
//                roots = arrayOf(KotlinSourceRoot("testdata/packages/classInPackage.kt", false)),
//                perPackageOptions = listOf(
//                    PackageOptionsImpl(prefix = "simple.name", suppress = true)
//                ),
//                analysisPlatform = analysisPlatform
//            )
//        ) { model ->
//            assertEquals(0, model.members.count())
//        }
//    }
}