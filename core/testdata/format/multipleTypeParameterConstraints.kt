interface A {

}

interface B {

}


fun f<T> where T : A, T : B {
}
