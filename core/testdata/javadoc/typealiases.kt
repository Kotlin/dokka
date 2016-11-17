class A

typealias B = A

class C : B

typealias D = (A) -> C

fun some(d: D) {

}