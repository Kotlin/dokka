package org.jetbrains.dokka.model.doc

sealed class TagWrapper(val root: DocTag) {

    override fun equals(other: Any?): Boolean =
        (
            other != null &&
            this::class == other::class &&
            this.root == (other as TagWrapper).root
        )

    override fun hashCode(): Int = root.hashCode()
}
class Description(root: DocTag) : TagWrapper(root)
class Author(root: DocTag) : TagWrapper(root)
class Version(root: DocTag) : TagWrapper(root)
class Since(root: DocTag) : TagWrapper(root)
class See(root: DocTag, val name: String) : TagWrapper(root) {
    override fun equals(other: Any?): Boolean = super.equals(other) && this.name == (other as See).name
    override fun hashCode(): Int = super.hashCode() + name.hashCode()
}
class Param(root: DocTag, val name: String) : TagWrapper(root)  {
    override fun equals(other: Any?): Boolean = super.equals(other) && this.name == (other as Param).name
    override fun hashCode(): Int = super.hashCode() + name.hashCode()
}
class Return(root: DocTag) : TagWrapper(root)
class Receiver(root: DocTag) : TagWrapper(root)
class Constructor(root: DocTag) : TagWrapper(root)
class Throws(root: DocTag, val name: String) : TagWrapper(root)  {
    override fun equals(other: Any?): Boolean = super.equals(other) && this.name == (other as Throws).name
    override fun hashCode(): Int = super.hashCode() + name.hashCode()
}
class Sample(root: DocTag, val name: String) : TagWrapper(root)  {
    override fun equals(other: Any?): Boolean = super.equals(other) && this.name == (other as Sample).name
    override fun hashCode(): Int = super.hashCode() + name.hashCode()
}
class Deprecated(root: DocTag) : TagWrapper(root)
class Property(root: DocTag, val name: String) : TagWrapper(root)  {
    override fun equals(other: Any?): Boolean = super.equals(other) && this.name == (other as Property).name
    override fun hashCode(): Int = super.hashCode() + name.hashCode()
}
class Suppress(root: DocTag) : TagWrapper(root)
class CustomWrapperTag(root: DocTag, val name: String) : TagWrapper(root)  {
    override fun equals(other: Any?): Boolean = super.equals(other) && this.name == (other as CustomWrapperTag).name
    override fun hashCode(): Int = super.hashCode() + name.hashCode()
}