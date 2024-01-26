/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

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
