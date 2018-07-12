package org.jetbrains.dokka.tests.model

import com.sun.tools.javac.util.BaseFileManager
import org.jetbrains.dokka.Content
import org.jetbrains.dokka.NodeKind
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.tests.BaseFunctionTest
import org.jetbrains.dokka.tests.ModelConfig
import org.jetbrains.dokka.tests.verifyPackageMember
import org.junit.Assert
import org.junit.Test

class JVMFunctionTest: BaseFunctionTest(Platform.jvm) {
    @Test
    fun annotatedFunction() {
        verifyPackageMember("testdata/functions/annotatedFunction.kt", ModelConfig(
            analysisPlatform = Platform.jvm,
            withKotlinRuntime = true
        )) { func ->
            Assert.assertEquals(1, func.annotations.count())
            with(func.annotations[0]) {
                Assert.assertEquals("Strictfp", name)
                Assert.assertEquals(Content.Empty, content)
                Assert.assertEquals(NodeKind.Annotation, kind)
            }
        }
    }

}