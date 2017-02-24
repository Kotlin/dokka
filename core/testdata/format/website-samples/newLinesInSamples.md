---
title: foo - test
layout: api
---

<div class='api-docs-breadcrumbs'><a href="test/index">test</a> / <a href="test/foo">foo</a></div>

# foo

<div class="signature"><code><span class="keyword">fun </span><span class="identifier">foo</span><span class="symbol">(</span><span class="symbol">)</span><span class="symbol">: </span><span class="identifier">Unit</span></code></div>
<div class="sample" markdown="1">

``` kotlin


fun main(args: Array<String>) {
//sampleStart
val words = listOf("a", "abc", "ab", "def", "abcd")
val byLength = words.groupBy { it.length }

println(byLength.keys) // [1, 3, 2, 4]
println(byLength.values) // [[a], [abc, def], [ab], [abcd]]

val mutableByLength: MutableMap<Int, MutableList<String>> = words.groupByTo(mutableMapOf()) { it.length }
// same content as in byLength map, but the map is mutable
println("mutableByLength == byLength is ${mutableByLength == byLength}") // true
//sampleEnd
}
```

</div>
