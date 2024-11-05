/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.utils

import io.kotest.assertions.shouldFail
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.throwable.shouldHaveMessage
import kotlin.io.path.deleteRecursively
import kotlin.io.path.writeText

class ShouldBeADirectoryWithSameContentAsTest : FunSpec({

    test("when expected directory doesn't exist, expect failure") {
        val expectedDir = tempDir()
        val actualDir = tempDir()

        expectedDir.deleteRecursively()

        val failure = shouldFail { expectedDir.shouldBeADirectoryWithSameContentAs(actualDir) }

        failure.shouldHaveMessage("expectedDir '$expectedDir' is not a directory (exists:false, file:false)")
    }

    test("when actual directory doesn't exist, expect failure") {
        val expectedDir = tempDir()
        val actualDir = tempDir()

        actualDir.deleteRecursively()

        val failure = shouldFail { expectedDir.shouldBeADirectoryWithSameContentAs(actualDir) }

        failure.shouldHaveMessage("actualDir '$actualDir' is not a directory (exists:false, file:false)")
    }

    test("when directories have different files, expect failure") {
        val expectedDir = tempDir().apply {
            resolve("file0.txt").writeText("valid file")
            resolve("file1.txt").writeText("not in actual")
            resolve("file2.txt").writeText("not in actual")
            resolve("file3.txt").writeText("not in actual")
        }

        val actualDir = tempDir().apply {
            resolve("file0.txt").writeText("valid file")
            resolve("file-a.txt").writeText("not in expected")
            resolve("file-b.txt").writeText("not in expected")
            resolve("file-c.txt").writeText("not in expected")
        }

        val failure = shouldFail { expectedDir.shouldBeADirectoryWithSameContentAs(actualDir) }

        failure.shouldHaveMessage(
            """
            actualDir is missing 3 files:
              - file1.txt
              - file2.txt
              - file3.txt
            actualDir has 3 unexpected files:
              - file-a.txt
              - file-b.txt
              - file-c.txt
            """.trimIndent()
        )
    }

    test("when file in directories has different content, expect failure with contents of file") {
        val expectedDir = tempDir()
        expectedDir.resolve("file.txt").writeText("content")

        val actualDir = tempDir()
        actualDir.resolve("file.txt").writeText("unexpected content")

        val failure = shouldFail { expectedDir.shouldBeADirectoryWithSameContentAs(actualDir) }

        failure.shouldHaveMessage(
            """
            file.txt has 1 differences in content:
                --- file.txt
                +++ file.txt
                @@ -1,1 +1,1 @@
                -content
                +unexpected content
            """.trimIndent()
        )
    }
})
