import java.io.Serializable

/**
 * A group of *members*.
 *
 * This class has no useful logic; it's just a documentation example.
 * This link [Serializable] shall render a javadoc reference.
 *
 * @param T the type of a member in this group.
 * @property name the *name* of this group.
 * @constructor Creates an empty group.
 */
class AsciidocTags<T>(val name: String) {
    /**
     * Adds a *member* to this group.
     *
     * @return the new size of the group.
     */
    fun add(member: T): Int = 0
}