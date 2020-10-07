# Configuration specific to HTML format

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

### Examples
In order to override a logo and style it accordingly a simple css file named `logo-styles.css` is needed:
```css
#logo {
    background-image: url('https://upload.wikimedia.org/wikipedia/commons/9/9d/Ubuntu_logo.svg');
    /* other styles required to make your page pretty */
}
```

For build system specific instructions please visit dedicated pages: [gradle](../gradle/usage.md#Applying plugins), [maven](../maven/usage.md#Applying plugins) and [cli](../cli/usage.md#Configuration options)