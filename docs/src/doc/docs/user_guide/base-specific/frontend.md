# Configuration specific to HTML format

## Prerequisites

Dokka's Html format requires a web server to view documentation correctly.
This can be achieved by using the one that is build in IntelliJ or providing your own.
If this requisite is not fulfilled Dokka with fail to load navigation pane and search bars.

!!! important
    Concepts specified below apply only to configuration of the Base Plugin (that contains Html format) 
    and needs to be applied via pluginsConfiguration and not on the root one.

## Modifying assets

It is possible to change static assets that are used to generate dokka's HTML. 
Currently, user can modify:
 
 * customAssets
 * customStyleSheets
 
Every file provided in those values will be applied to **every** page.

Dokka uses 3 stylesheets:

* `style.css` - main css file responsible for styling the page
* `jetbrains-mono.css` - fonts used across dokka
* `logo-styles.css` - logo styling

User can choose to add or override those files. 
Resources will be overridden when in `pluginConfiguration` block there is a resource with the same name.

## Modifying footer

Dokka supports custom messages in the footer via `footerMessage` string property on base plugin configuration. 
Keep in mind that this value will be pased exactly to the output html, so it has to be valid and escaped correctly.

## Separating inherited members

By setting a boolean property `separateInheritedMembers` dokka will split inherited members (like functions, properties etc.) 
from ones declared in viewed class. Separated members will have it's own tabs on the page.

### Examples
In order to override a logo and style it accordingly a simple css file named `logo-styles.css` is needed:
```css
#logo {
    background-image: url('https://upload.wikimedia.org/wikipedia/commons/9/9d/Ubuntu_logo.svg');
    /* other styles required to make your page pretty */
}
```

For build system specific instructions please visit dedicated pages: [gradle](../gradle/usage.md#Applying plugins), [maven](../maven/usage.md#Applying plugins) and [cli](../cli/usage.md#Configuration options)
