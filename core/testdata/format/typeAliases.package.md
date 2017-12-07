[test](./index.md)

## Package &lt;root&gt;

### Types

| [A](-a/index.md) | `class A` |
| [B](-b/index.md) | `class B` |
| [C](-c/index.md) | `class C<T>` |

### Type Aliases

| [D](-d.md) | `typealias D = `[`A`](-a/index.md) |
| [E](-e.md) | `typealias E = `[`D`](-d.md) |
| [F](-f.md) | `typealias F = (`[`A`](-a/index.md)`) -> `[`B`](-b/index.md) |
| [G](-g.md) | `typealias G = `[`C`](-c/index.md)`<`[`A`](-a/index.md)`>` |
| [H](-h.md) | `typealias H<T> = `[`C`](-c/index.md)`<`[`T`](-h.md#T)`>` |
| [I](-i.md) | `typealias I<T> = `[`H`](-h.md)`<`[`T`](-i.md#T)`>` |
| [J](-j.md) | `typealias J = `[`H`](-h.md)`<`[`A`](-a/index.md)`>` |
| [K](-k.md) | `typealias K = `[`H`](-h.md)`<`[`J`](-j.md)`>` |
| [L](-l.md) | `typealias L = (`[`K`](-k.md)`, `[`B`](-b/index.md)`) -> `[`J`](-j.md) |
| [M](-m.md) | `typealias M = `[`A`](-a/index.md)<br>Documented |
| [N](-n.md) | `typealias ~~N~~ = `[`A`](-a/index.md) |

