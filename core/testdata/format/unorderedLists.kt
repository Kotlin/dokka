/**
 * Usage summary:
 *
 * - Rinse
 * - Repeat
 *
 * Usage instructions:
 *
 * - [Bar.rinse] to rinse
 * - Alter any rinse options _(optional)_
 * - To repeat; [Bar.repeat]
 *      - Can reconfigure options:
 *          - Soap
 *          - Elbow Grease
 *          - Bleach
 *
 * Rinse options:
 *
 * - [Bar.useSoap]
 *      - _recommended_
 *
 * - [Bar.useElbowGrease]
 *      - _warning: requires effort_
 *
 * - [Bar.useBleach]
 *      - __use with caution__
 *
 */
class Bar {
    fun rinse() = Unit
    fun repeat() = Unit

    var useSoap = false
    var useElbowGrease = false
    var useBleach = false
}
