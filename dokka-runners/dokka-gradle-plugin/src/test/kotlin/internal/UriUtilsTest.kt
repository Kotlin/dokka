package internal

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jetbrains.dokka.gradle.internal.appendPath
import java.net.URI

class UriUtilsTest : FunSpec({
    test("appending path with space should be escaped") {
        val base = URI("http://localhost:8080/docs/")
        val addition = "/Foo Bar/x/y/z/index.html"

        val result = base.appendPath(addition).toString()
        result shouldBe "http://localhost:8080/docs/Foo%20Bar/x/y/z/index.html"
    }
})
