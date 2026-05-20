package a.b.c

/** A class defined in `commonMain`. */
class CommonMainCls

/**
 * A `commonMain` function that uses:
 * - [CommonMainCls] from `commonMain`
 */
fun commpnMainFn(
    a: CommonMainCls,
) {
    println(a)
}
