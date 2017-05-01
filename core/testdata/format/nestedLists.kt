/**
 * Usage instructions:
 *
 * - __Rinse__
 *      1. Alter any rinse options _(optional)_
 *          - Recommended to [Bar.useSoap]
 *          - Optionally apply [Bar.elbowGrease] for best results
 *      2. [Bar.rinse] to begin rinse
 *          1. Thus you should call [Bar.rinse]
 *          2. *Then* call [Bar.repeat]
 *              - Don't forget to use:
 *                  - Soap
 *                  - Elbow Grease
 *          3. Finally, adjust soap usage [Bar.useSoap] as needed
 *      3. Repeat with [Bar.repeat]
 *
 * - __Repeat__
 *      - Will use previously used rinse options
 *      - [Bar.rinse] must have been called once before
 *      - Can be repeated any number of times
 *      - Options include:
 *          - [Bar.useSoap]
 *          - [Bar.useElbowGrease]
 */
class Bar {
    fun rinse() = Unit
    fun repeat() = Unit

    var useSoap = false
    var useElbowGrease = false
}
