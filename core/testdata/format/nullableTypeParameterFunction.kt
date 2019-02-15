class Bar<T> {
    val dataList = mutableListOf<T>()

    open fun checkElement(
        elem: T,
        addFunc: ((elem: T) -> Unit)? = { dataList.add(it) }
    ): Int = 1
}