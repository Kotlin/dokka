package a.b.c

/** A class defined in `linuxMain`. */
class LinuxMainCls

/**
 * A `linuxMain` function that uses:
 * - [CommonMainCls] from `commonMain`
 * - [NativeMainCls] from `nativeMain`
 * - [LinuxMainCls] from `linuxMain`
 */
fun linuxFn(
    a: CommonMainCls,
    b: NativeMainCls,
    c: LinuxMainCls,
) {
    println(a)
    println(b)
    println(c)
}
