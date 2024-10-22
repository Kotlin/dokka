package it.mpp.second

import it.mpp.first.*

/**
 * exposes [Subclass] from another module which has [SubclassOptInRequired] annotation present
 */
class Usage(
    val subclass: Subclass
)
