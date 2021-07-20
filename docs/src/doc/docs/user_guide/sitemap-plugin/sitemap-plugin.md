#Sitemap plugin

Plugin creates a sitemap for the documentation in text format according to 
the [specification](https://developers.google.com/search/docs/advanced/sitemaps/build-sitemap?hl=en&visit_id=637613321436192601-2699040246&rd=1#text).

### Prerequisites
Sitemap plugin works with html format and has to be applied manually for each documented module.

### Configuration
Sitemap plugin can be configured using a plugins configuration mechanism 
described on build tool's page: [gradle](../gradle/usage.md#applying-plugins),
[maven](../maven/usage.md#applying-plugins), [cli](../cli/usage.md#applying-plugins).

Accepted configuration options are:

* baseUrl - url of the website you would like to host your documentation on.
* relativeOutputLocation - desired location of the output file. 

Both of those parameters are optional, if not specified otherwise defaults will be taken:

* baseUrl - this will be left blank with a warning emitted in console. 
It is user's responsibility to prefix all entries with domain name
* relativeOutputLocation - `sitemap.txt`

