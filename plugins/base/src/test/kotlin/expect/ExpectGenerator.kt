package expect

import kotlin.test.Ignore
import kotlin.test.Test

class ExpectGenerator : AbstractExpectTest() {

    @Ignore
    @Test
    fun generateAll() = testDir?.dirsWithFormats(formats).orEmpty().forEach { (p, f) ->
        generateExpect(p, f)
    }
}
