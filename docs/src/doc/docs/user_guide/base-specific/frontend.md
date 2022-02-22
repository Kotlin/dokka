# Configuration specific to HTML format

## Prerequisites

Dokka's HTML format requires a web server to view documentation correctly.
This can be achieved by using the one that is build in IntelliJ or providing your own.
If this requisite is not fulfilled Dokka with fail to load navigation pane and search bars.

!!! important
    Concepts specified below apply only to configuration of the Base Plugin (that contains HTML format) 
    and needs to be applied via pluginsConfiguration and not on the root one.

## Modifying assets

It is possible to change static assets that are used to generate dokka's HTML. 
Currently, user can modify:
 
 * customAssets
 * customStyleSheets
 
Every file provided in those values will be applied to **every** page.

Dokka uses 4 stylesheets:

* `style.css` - main css file responsible for styling the page
* `jetbrains-mono.css` - fonts used across dokka
* `logo-styles.css` - logo styling
* [`prism.css`](https://github.com/Kotlin/dokka/blob/master/plugins/base/src/main/resources/dokka/styles/prism.css) - code highlighting

Also, it uses js scripts. The actual ones are [here](https://github.com/Kotlin/dokka/tree/master/plugins/base/src/main/resources/dokka/scripts).
User can choose to add or override those files. 
Resources will be overridden when in `pluginConfiguration` block there is a resource with the same name.

## Modifying footer

Dokka supports custom messages in the footer via `footerMessage` string property on base plugin configuration. 
Keep in mind that this value will be passed exactly to the output HTML, so it has to be valid and escaped correctly.

## Separating inherited members

By setting a boolean property `separateInheritedMembers` dokka will split inherited members (like functions, properties etc.) 
from ones declared in viewed class. Separated members will have it's own tabs on the page.

## Merging declarations with name clashing 

By setting a boolean property `mergeImplicitExpectActualDeclarations` dokka will merge declarations that do not have `expect`/`actual` keywords but have the same fully qualified name. 
The declarations will be displayed on one page.
By default, it is disabled. The page names of such declaration have a prefix that is the name of source set. 

### Examples
In order to override a logo and style it accordingly a css file named `logo-styles.css` is needed:
```css
.library-name a {
    position: relative;
    --logo-width: 100px;
    margin-left: calc(var(--logo-width) + 5px);
}

.library-name a::before {
    content: '';
    background: url("https://upload.wikimedia.org/wikipedia/commons/9/9d/Ubuntu_logo.svg") center no-repeat;
    background-size: contain;
    position: absolute;
    width: var(--logo-width);
    height: 50px;
    top: -18px;
    left: calc(-1 * var(--logo-width) - 5px);
    /* other styles required to make your page pretty */
}
```


For build system specific instructions please visit dedicated pages: [gradle](../gradle/usage.md#applying-plugins), [maven](../maven/usage.md#applying-plugins) and [cli](../cli/usage.md#configuration-options)

## Custom HTML pages

Dokka uses [FreeMarker](https://freemarker.apache.org/) template engine to render pages. 
It takes templates from a folder that is set by a property `templatesDir`.
To custom HTML output user can use a [default template](https://github.com/Kotlin/dokka/blob/master/plugins/base/src/main/resources/dokka/templates) as a basic.

!!! note
    To change page assets user can set properties `customAssets` and `customStyleSheets`.
    Assets are handled by Dokka.

Currently, there is one template file `base.ftl`. It defines general design of all pages to render.  

Variables given below are available to a template:
  - `${pageName}` - a page name
  - `${footerMessage}` - a text is set by `footerMessage` property
  - `${sourceSets}` - a nullable list of source set, only for multi-platform pages. Each source set has `name`, `platfrom` and `filter` properties.

Also, Dokka-defined [directives](https://freemarker.apache.org/docs/ref_directive_userDefined.html) can be used:
  - `<@content/>` - a main content
  - `<@resources/>` - scripts, stylesheets 
  - `<@version/>` - version (A version plugin replace this with a version navigator)
  - `<@template_cmd name="...""> ...</@template_cmd>` - is used for stuff (`pathToRoot`, `projectName` are `name` parameter, local variables as well) that dependent on a root project. This is processed by a multi-module task that assembles a partial outputs from modules. 
     Example:
    ```
    <@template_cmd name="projectName">
       <span>${projectName}</span>
    </@template_cmd>
    ```