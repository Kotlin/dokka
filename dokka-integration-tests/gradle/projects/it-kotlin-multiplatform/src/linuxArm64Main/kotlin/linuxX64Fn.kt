package a.b.c

/**
 * A `linuxArm64Main` function that uses:
 * - [CommonMainCls] from `commonMain`
 * - [NativeMainCls] from `nativeMain`
 * - [LinuxMainCls] from `linuxMain`
 */
fun linuxArm64Fn(
    a: CommonMainCls,
    b: NativeMainCls,
    c: LinuxMainCls,
) {
    println(a)
    println(b)
    println(c)
}
