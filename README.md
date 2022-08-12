# Ktor OpenAPI Generator

The Ktor OpenAPI Generator is a library to automatically generate the descriptor as you route your ktor application.

_This version by [LukasForst](https://github.com/LukasForst) was migrated to Ktor `2.0.0` and is hosted on Maven
Central_.

```kotlin
dependencies {
    implementation("dev.forst", "ktor-openapi-generator", "0.4.5")
}
```

## Minimal Example

See [MinimalExample.kt](src/test/kotlin/MinimalExample.kt) for minimal example app and
then [MinimalExampleTest.kt](src/test/kotlin/MinimalExampleTest.kt) that it actually works.

```kotlin
/**
 * Minimal example of OpenAPI plugin for Ktor.
 */
fun Application.minimalExample() {
    // install OpenAPI plugin
    install(OpenAPIGen) {
        // this automatically servers Swagger UI on /swagger-ui
        serveSwaggerUi = true
        info {
            title = "Minimal Example API"
        }
    }
    // install JSON support
    install(ContentNegotiation) {
        jackson()
    }
    // add basic routes for openapi.json and redirect to UI
    routing {
        // serve openapi.json
        get("/openapi.json") {
            call.respond(this@routing.application.openAPIGen.api.serialize())
        }
        // and do redirect to make it easier to remember
        get("/swagger-ui") {
            call.respondRedirect("/swagger-ui/index.html?url=/openapi.json", true)
        }
    }
    // and now example routing
    apiRouting {
        route("/example/{name}") {
            // SomeParams are parameters (query or path), SomeResponse is what the backend returns and SomeRequest
            // is what was passed in the body of the request
            post<SomeParams, SomeResponse, SomeRequest> { params, someRequest ->
                respond(SomeResponse(bar = "Hello ${params.name}! From body: ${someRequest.foo}."))
            }
        }
    }
}

data class SomeParams(@PathParam("who to say hello") val name: String)
data class SomeRequest(val foo: String)
data class SomeResponse(val bar: String)
```

## About

Ktor OpenAPI Generator is:

- Modular
- Strongly typed
- Explicit

Currently Supported:

- Authentication interoperability with strongly typed Principal (OAuth only, see TestServer in tests)
- Content Negotiation interoperability (see TestServer in tests)
- Custom response codes (as parameter in `@Response`)
- Automatic and custom content Type routing and parsing (see `com.papsign.ktor.openapigen.content.type`, Binary Parser
  and default JSON parser (that uses the ktor implicit parsing/serializing))
- Exception handling (use `.throws(ex) {}` in the routes with an APIException object) with Status pages interop (with
  .withAPI in the StatusPages configuration)
- tags (`.tag(tag) {}` in route with a tag object, currently must be an enum, but may be subject to change)
- Spec compliant Parameter Parsing (see basic example)
- Legacy Polymorphism with use of `@DiscriminatorAnnotation()` attribute and sealed classes

Extra Features:

- Includes Swagger-UI (enabled by default, can be managed in the `install(OpenAPIGen) { ... }` section)

## Examples

Take a look at [a few examples](https://github.com/papsign/Ktor-OpenAPI-Generator/wiki/A-few-examples)
