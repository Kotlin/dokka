package bar

/**
 * Just a fruit
 *
 * @param weight in grams
 * @param ranking quality from 0 to 10, where 10 is best
 * @param color yellow is default
 */
class Banana (
    private val weight: Double,
    private val ranking: Int,
    color: String = "yellow"
)