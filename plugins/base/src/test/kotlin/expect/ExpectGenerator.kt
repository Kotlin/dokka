package expect

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class ExpectGenerator : AbstractExpectTest() {

    @Disabled
    @Test
    fun generateAll() = testDir?.dirsWithFormats(formats).orEmpty().forEach { (p, f) ->
        generateExpect(p, f)
    }
}