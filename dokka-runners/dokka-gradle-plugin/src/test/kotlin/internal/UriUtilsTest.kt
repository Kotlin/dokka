package internal

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import org.jetbrains.dokka.gradle.internal.appendPath
import java.net.URI

class UriUtilsTest : FunSpec({
    test("appending path with space should be escaped") {
        val base = URI("http://localhost:8080/docs/")
        val addition = "/Foo Bar/<x^y>z/#the%homepage.html"

        val result = base.appendPath(addition).toString()
        result shouldBe "http://localhost:8080/docs/Foo%20Bar/%3Cx%5Ey%3Ez/%23the%25homepage.html"
    }

    test("check any path can be appended to a URI without throwing exception") {
        // generate strings to be/joined/into/a/path.
        val pathElementsArb = Arb.list(
            gen = Arb.string(minSize = 1, maxSize = 10),
            range = 1..5,
        )

        checkAll(pathElementsArb) { pathElements ->
            val path = pathElements.joinToString("/")
            val base = URI("http://localhost:8080/docs/")
            shouldNotThrowAny {
                base.appendPath(path)
            }
        }
    }
})
