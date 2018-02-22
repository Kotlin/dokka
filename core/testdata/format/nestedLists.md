[test](../index.md) / [Bar](./index.md)

# Bar

`class Bar`

Usage instructions:

* **Rinse**
  1. Alter any rinse options *(optional)*
       * Recommended to [Bar.useSoap](use-soap.md)
       * Optionally apply [Bar.elbowGrease](#) for best results
  2. [Bar.rinse](rinse.md) to begin rinse
       1. Thus you should call [Bar.rinse](rinse.md)
       2. *Then* call [Bar.repeat](repeat.md)
           * Don't forget to use:
              * Soap
              * Elbow Grease
       3. Finally, adjust soap usage [Bar.useSoap](use-soap.md) as needed
  3. Repeat with [Bar.repeat](repeat.md)

* **Repeat**
  * Will use previously used rinse options
  * [Bar.rinse](rinse.md) must have been called once before
  * Can be repeated any number of times
  * Options include:
      * [Bar.useSoap](use-soap.md)
      * [Bar.useElbowGrease](use-elbow-grease.md)

### Constructors

| [&lt;init&gt;](-init-.md) | `Bar()`<br>Usage instructions: |

### Properties

| [useElbowGrease](use-elbow-grease.md) | `var useElbowGrease: Boolean` |
| [useSoap](use-soap.md) | `var useSoap: Boolean` |

### Functions

| [repeat](repeat.md) | `fun repeat(): Unit` |
| [rinse](rinse.md) | `fun rinse(): Unit` |

