# Custom Dokka Plugin example

This project demonstrates how to create and configure a custom Dokka plugin using DGPv2.

For more details about Dokka plugin development, see the Dokka developer docs:
https://kotlin.github.io/dokka/2.1.0/developer_guide/plugin-development/introduction/

## Details

This example contains two subprojects:

- [dokka-plugin-hide-internal-api](dokka-plugin-hide-internal-api) contains the custom
  `HideInternalApiPlugin` Dokka plugin.
- [demo-library](demo-library) demonstrates how `HideInternalApiPlugin` can be configured and used.
