package a.b.c

/** A class defined in `nativeMain`. */
class NativeMainCls

/**
 * A `nativeMain` function that uses:
 * - [CommonMainCls] from `commonMain`
 * - [NativeMainCls] from `nativeMain`
 */
fun linuxFn(
    a: CommonMainCls,
    b: NativeMainCls,
) {
    println(a)
    println(b)
}
