@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
public annotation class Fancy(val size: Int) {

        override fun equals(other: Any?): Boolean {
            return super.equals(other)
        }

        override fun hashCode(): Int {
            return super.hashCode()
        }

        override fun toString(): String {
            return super.toString()
        }
}

@Fancy(1) fun f() {}
