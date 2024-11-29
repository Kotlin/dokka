package a.b.c

/**
 * A `jvmMain` function that uses:
 * - [CommonMainCls] from `commonMain`
 * - [JvmMainCls] from `jvmMain`
 */
fun jvmMainFn(a: CommonMainCls, b: JvmMainCls) {
    println(a)
    println(b)
}
