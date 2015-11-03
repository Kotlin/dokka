import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.TYPE, ElementType.METHOD})
public @interface Attribute {
  String value() default "";
}
