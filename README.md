# Ktor OpenAPI Generator

The Ktor OpenAPI Generator is a library to automatically generate the descriptor as you route your ktor application.

_This version by [LukasForst](https://github.com/LukasForst) was migrated to Ktor `2.0.0` and is hosted on Maven Central_.

```kotlin
dependencies { 
    implementation("dev.forst", "ktor-openapi-generator", "0.4.3")
}
```

Ktor OpenAPI Generator is:
- Modular
- Strongly typed
- Explicit

Currently Supported:
- Authentication interoperability with strongly typed Principal (OAuth only, see TestServer in tests)
- Content Negotiation interoperability (see TestServer in tests)
- Custom response codes (as parameter in `@Response`)
- Automatic and custom content Type routing and parsing (see `com.papsign.ktor.openapigen.content.type`, Binary Parser and default JSON parser (that uses the ktor implicit parsing/serializing))
- Exception handling (use `.throws(ex) {}` in the routes with an APIException object) with Status pages interop (with .withAPI in the StatusPages configuration)
- tags (`.tag(tag) {}` in route with a tag object, currently must be an enum, but may be subject to change)
- Spec compliant Parameter Parsing (see basic example)
- Legacy Polymorphism with use of `@DiscriminatorAnnotation()` attribute and sealed classes 

Extra Features:
- Includes Swagger-UI (enabled by default, can be managed in the `install(OpenAPIGen) { ... }` section)

## Examples

Take a look at [a few examples](https://github.com/papsign/Ktor-OpenAPI-Generator/wiki/A-few-examples)
