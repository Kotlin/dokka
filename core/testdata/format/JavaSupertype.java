public class JavaSupertype {
  public static class Foo {
  }

  public static class Bar extends Foo {
    public Foo returnFoo(Foo foo) { return foo; }
  }
}
