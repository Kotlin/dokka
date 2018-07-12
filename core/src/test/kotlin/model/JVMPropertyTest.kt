package org.jetbrains.dokka.tests.model

import org.jetbrains.dokka.Content
import org.jetbrains.dokka.NodeKind
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.tests.BasePropertyTest
import org.jetbrains.dokka.tests.ModelConfig
import org.jetbrains.dokka.tests.checkSourceExistsAndVerifyModel
import org.junit.Assert
import org.junit.Test

class JVMPropertyTest : BasePropertyTest(Platform.jvm) {
    @Test
    fun annotatedProperty() {
        checkSourceExistsAndVerifyModel(
            "testdata/properties/annotatedProperty.kt",
            modelConfig = ModelConfig(
                analysisPlatform = analysisPlatform,
                withKotlinRuntime = true
            )
        ) { model ->
            with(model.members.single().members.single()) {
                Assert.assertEquals(1, annotations.count())
                with(annotations[0]) {
                    Assert.assertEquals("Volatile", name)
                    Assert.assertEquals(Content.Empty, content)
                    Assert.assertEquals(NodeKind.Annotation, kind)
                }
            }
        }
    }

}