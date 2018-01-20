package p;


public class ConstJava {

    public static final String myStringConst = "";
    public static final int myIntConst = 0;

    public static final ConstJava myConstObjConst = new ConstJava(); // Not a constant, as it have not primitive type
}