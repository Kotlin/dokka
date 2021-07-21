package shouldbesuppressed

/**
 * This function should not be visible
 */
fun thatShouldNotBeVisible(): String = "oops"

/**
 * This class should not be visible
 */
class DontLookAtMe(val stealth: Int = 9001)