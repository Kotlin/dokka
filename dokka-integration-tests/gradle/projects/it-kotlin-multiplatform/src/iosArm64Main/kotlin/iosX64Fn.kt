package a.b.c

/**
 * A `iosX64Main` function that uses:
 * - [CommonMainCls] from `commonMain`
 * - [NativeMainCls] from `nativeMain`
 * - [IosMainCls] from `iosMain`
 */
fun iosX64MainFn(
    a: CommonMainCls,
    b: NativeMainCls,
    c: IosMainCls,
) {
    println(a)
    println(b)
    println(c)
}
