@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
public annotation class Fancy(val size: Int)


@Fancy(1) fun f() {}
