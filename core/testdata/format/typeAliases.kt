
class A
class B
class C<T>

typealias D = A
typealias E = D

typealias F = (A) -> B

typealias G = C<A>
typealias H<T> = C<T>

typealias I<T> = H<T>
typealias J = H<A>

typealias K = H<J>

typealias L = (K, B) -> J

/**
 * Documented
 */
typealias M = A

@Deprecated("!!!")
typealias N = A