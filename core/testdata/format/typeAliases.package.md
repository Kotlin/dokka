[test](test/index)

## Package &lt;root&gt;

### Types

| [A](test/-a/index) | `class A` |
| [B](test/-b/index) | `class B` |
| [C](test/-c/index) | `class C<T>` |

### Type Aliases

| [D](test/-d) | `typealias D = `[`A`](test/-a/index) |
| [E](test/-e) | `typealias E = `[`D`](test/-d) |
| [F](test/-f) | `typealias F = (`[`A`](test/-a/index)`) -> `[`B`](test/-b/index) |
| [G](test/-g) | `typealias G = `[`C`](test/-c/index)`<`[`A`](test/-a/index)`>` |
| [H](test/-h) | `typealias H<T> = `[`C`](test/-c/index)`<`[`T`](test/-h#T)`>` |
| [I](test/-i) | `typealias I<T> = `[`H`](test/-h)`<`[`T`](test/-i#T)`>` |
| [J](test/-j) | `typealias J = `[`H`](test/-h)`<`[`A`](test/-a/index)`>` |
| [K](test/-k) | `typealias K = `[`H`](test/-h)`<`[`J`](test/-j)`>` |
| [L](test/-l) | `typealias L = (`[`K`](test/-k)`, `[`B`](test/-b/index)`) -> `[`J`](test/-j) |
| [M](test/-m) | `typealias M = `[`A`](test/-a/index)<br>Documented |
| [N](test/-n) | `typealias ~~N~~ = `[`A`](test/-a/index) |

