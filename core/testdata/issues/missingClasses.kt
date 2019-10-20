
class Test
    (var value: MissingClassInConstructor)
{
    fun method1(): MissingClassInDeclaredReturnType = MissingClassInDeclaredReturnType()

    fun method2() = MissingClassInInferredReturnType().getSomeOtherProperty() + "appended expression value"

    fun method3(input: MissingClassInMethodParameter) { }

    fun <T : MissingClassInTypeParameter> method4(input: T) { }

    fun method5() : List<MissingClassAsGeneric> { return emptyList() }

    fun MissingClassReceiver.method6() { }

    fun method7(block: MissingClassLambdaReceiver.()->Unit) { }

    fun method8(block: (MissingClassLambdaParameter)->Unit) { }

    fun method9(block: ()->MissingClassLambdaReturnType) { }

    val prop1: MissingClassInDeclaredPropertyType = MissingClassInDeclaredPropertyType()

    val prop2 = MissingClassInInferredPropertyType()

    val prop3 get() = MissingClassInInferredPropertyGetterType()

}
