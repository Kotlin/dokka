package org.jetbrains.dokka

public class RichString {
    private val sliceList = arrayListOf<RichStringSlice>()
    public val slices: List<RichStringSlice> get() = sliceList

    public fun addSlice(slice: RichStringSlice) {
        sliceList.add(slice)
    }

    public fun addSlice(text: String, style: RichStringStyle) {
        // TODO: decide on semantics
        // empty slices makes it hard to compare rich strings
        if (text.length > 0)
            sliceList.add(RichStringSlice(text, style))
    }

    public fun isEmpty(): Boolean = sliceList.isEmpty()

    public fun length(): Int = sliceList.fold(0) {(acc, value) -> return acc + value.text.length }

    public override fun toString(): String {
        return sliceList.joinToString("", "&")
    }

    override fun equals(other: Any?): Boolean {
        if (other !is RichString)
            return false
        if (sliceList.size != other.sliceList.size)
            return false
        for (index in sliceList.indices)
            if (!sliceList[index].equals(other.sliceList[index]))
                return false

        return true
    }

    override fun hashCode(): Int {
        return sliceList.map { it.hashCode() }.sum()
    }

    class object {
        public val empty: RichString = RichString()
    }
}

public data class RichStringSlice(public val text: String, public val style: RichStringStyle) {
    public override fun toString(): String {
        return text
    }
}

public trait RichStringStyle
public object NormalStyle : RichStringStyle
public object BoldStyle : RichStringStyle
public object CodeStyle : RichStringStyle
public data class LinkStyle(val link: String) : RichStringStyle

public fun RichString.splitBy(delimiter: String): Pair<RichString, RichString> {
    var index = 0
    while (index < slices.size && !slices[index].text.contains(delimiter)) index++
    if (index == slices.size)
        return Pair(this, RichString.empty)

    val first = RichString()
    val second = RichString()

    for (i in 0..index - 1) {
        first.addSlice(slices[i])
    }

    val splitSlice = slices[index]
    val firstText = splitSlice.text.substringBefore(delimiter)
    val secondText = splitSlice.text.substringAfter(delimiter)
    first.addSlice(firstText, splitSlice.style)
    second.addSlice(secondText, splitSlice.style)

    for (i in index + 1..slices.size - 1) {
        second.addSlice(slices[i])
    }

    return Pair(first, second)
}
