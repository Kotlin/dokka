class FireException : Exception


/**
 * COMM
 * @param a Some string
 * @return value of a
 * @throws FireException in case of fire
 */
@Throws(FireException::class)
fun my(a: String): String = a

