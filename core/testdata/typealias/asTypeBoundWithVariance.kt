package _typealias.astypebound
class A

typealias B = A

class C<out T : B>
class D<in T : B>