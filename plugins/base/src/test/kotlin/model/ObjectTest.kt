package model

import org.jetbrains.dokka.model.AdditionalModifiers
import org.jetbrains.dokka.model.DObject
import org.jetbrains.dokka.model.ExtraModifiers
import org.junit.jupiter.api.Test
import utils.AbstractModelTest

class ObjectTest : AbstractModelTest("/src/main/kotlin/objects/Test.kt", "objects") {

    @Test
    fun emptyObject() {
        inlineModelTest(
            """
            |object Obj {}
            """.trimIndent()
        ) {
            with((this / "objects" / "Obj").cast<DObject>()) {
                name equals "Obj"
                children counts 3
            }
        }
    }

    @Test
    fun `data object class`() {
        inlineModelTest(
            """
            |data object KotlinDataObject {}
            """.trimIndent()
        ) {
            val dataObject = packages.flatMap { it.classlikes }.first() as DObject
            dataObject.name equals "KotlinDataObject"
            dataObject.extra[AdditionalModifiers]?.content?.values?.single()
                ?.single() equals ExtraModifiers.KotlinOnlyModifiers.Data
        }
    }
}
