@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
public annotation class Fancy

fun function(@Fancy notInlined: () -> Unit) {
}
