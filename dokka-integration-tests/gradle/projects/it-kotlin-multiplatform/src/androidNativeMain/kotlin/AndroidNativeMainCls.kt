package a.b.c

/** A class defined in `AndroidNativeMain`. */
class AndroidNativeMainCls

/**
 * A `linuxMain` function that uses:
 * - [CommonMainCls] from `commonMain`
 * - [NativeMainCls] from `nativeMain`
 * - [AndroidNativeMainCls] from `AndroidNativeMain`
 */
fun AndroidNativeFn(
    a: CommonMainCls,
    b: NativeMainCls,
    c: AndroidNativeMainCls,
) {
    println(a)
    println(b)
    println(c)
}
