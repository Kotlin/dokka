package foo

abstract class Apple {
    protected var name: String = "foo"
    internal var weight: Int = 180
    var rating: Int = 10
    private var color: String = "red"

    companion object {
        @JvmStatic
        val code : Int = 123456
    }


}