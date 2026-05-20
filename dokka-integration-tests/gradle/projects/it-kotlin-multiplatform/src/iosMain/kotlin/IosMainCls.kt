package a.b.c

/** A class defined in `iosMain`. */
class IosMainCls

/**
 * A `iosMain` function that uses:
 * - [CommonMainCls] from `commonMain`
 * - [NativeMainCls] from `nativeMain`
 * - [IosMainCls] from `iosMain`
 */
fun iosMainFn(
    a: CommonMainCls,
    b: NativeMainCls,
    c: IosMainCls,
) {
    println(a)
    println(b)
    println(c)
}
