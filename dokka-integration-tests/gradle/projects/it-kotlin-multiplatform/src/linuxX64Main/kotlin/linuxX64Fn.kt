package a.b.c

/**
 * A `linuxX64Main` function that uses:
 * - [CommonMainCls] from `commonMain`
 * - [NativeMainCls] from `nativeMain`
 * - [LinuxMainCls] from `linuxMain`
 */
fun linuxX64Fn(
    a: CommonMainCls,
    b: NativeMainCls,
    c: LinuxMainCls,
) {
    println(a)
    println(b)
    println(c)
}
