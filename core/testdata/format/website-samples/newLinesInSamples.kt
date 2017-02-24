fun groupBySample() {
    val words = listOf("a", "abc", "ab", "def", "abcd")
    val byLength = words.groupBy { it.length }

    assertPrints(byLength.keys, "[1, 3, 2, 4]")
    assertPrints(byLength.values, "[[a], [abc, def], [ab], [abcd]]")

    val mutableByLength: MutableMap<Int, MutableList<String>> = words.groupByTo(mutableMapOf()) { it.length }
    // same content as in byLength map, but the map is mutable
    assertTrue(mutableByLength == byLength)
}


/**
 * @sample groupBySample
 */
fun foo() {

}