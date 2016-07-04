[test](test/index) / [Throws](test/-throws/index)

# Throws

`class Throws`

This annotation indicates what exceptions should be declared by a function when compiled to a JVM method.

Example:

```
Throws(IOException::class)
fun readFile(name: String): String {...}
```

### Constructors

| [&lt;init&gt;](test/-throws/-init-) | `Throws()`<br>This annotation indicates what exceptions should be declared by a function when compiled to a JVM method. |

