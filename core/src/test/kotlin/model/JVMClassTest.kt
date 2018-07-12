package org.jetbrains.dokka.tests.model

import org.jetbrains.dokka.Content
import org.jetbrains.dokka.NodeKind
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.tests.BaseClassTest
import org.jetbrains.dokka.tests.ModelConfig
import org.jetbrains.dokka.tests.checkSourceExistsAndVerifyModel
import org.jetbrains.dokka.tests.verifyPackageMember
import org.junit.Assert
import org.junit.Test

class JVMClassTest: BaseClassTest(Platform.jvm) {
    @Test
    fun annotatedClass() {
        verifyPackageMember("testdata/classes/annotatedClass.kt", ModelConfig(
            analysisPlatform = analysisPlatform,
            withKotlinRuntime = true
        )
        ) { cls ->
            Assert.assertEquals(1, cls.annotations.count())
            with(cls.annotations[0]) {
                Assert.assertEquals("Strictfp", name)
                Assert.assertEquals(Content.Empty, content)
                Assert.assertEquals(NodeKind.Annotation, kind)
            }
        }
    }


    @Test fun javaAnnotationClass() {
        checkSourceExistsAndVerifyModel(
            "testdata/classes/javaAnnotationClass.kt",
            modelConfig = ModelConfig(analysisPlatform = analysisPlatform, withJdk = true)
        ) { model ->
            with(model.members.single().members.single()) {
                Assert.assertEquals(1, annotations.count())
                with(annotations[0]) {
                    Assert.assertEquals("Retention", name)
                    Assert.assertEquals(Content.Empty, content)
                    Assert.assertEquals(NodeKind.Annotation, kind)
                    with(details[0]) {
                        Assert.assertEquals(NodeKind.Parameter, kind)
                        Assert.assertEquals(1, details.count())
                        with(details[0]) {
                            Assert.assertEquals(NodeKind.Value, kind)
                            Assert.assertEquals("RetentionPolicy.SOURCE", name)
                        }
                    }
                }
            }
        }
    }

}