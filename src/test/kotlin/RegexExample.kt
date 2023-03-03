import com.papsign.ktor.openapigen.OpenAPIGen
import com.papsign.ktor.openapigen.annotations.type.string.pattern.RegularExpression
import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation

/**
 * Example of using regex validator.
 */
fun Application.regexExample() {
    // install OpenAPI plugin
    install(OpenAPIGen) {
        // this servers OpenAPI definition on /openapi.json
        serveOpenApiJson = true
        // this servers Swagger UI on /swagger-ui/index.html
        serveSwaggerUi = true
        info {
            title = "Minimal Example API"
        }
    }
    // install JSON support
    install(ContentNegotiation) {
        jackson()
    }
    // and now example routing
    apiRouting {
        route("/example") {
            post<Unit, RegexExampleResponse, RegexExampleRequest> {_, _ ->
                respond(RegexExampleResponse("ok"))
            }
        }
    }
}

data class RegexExampleRequest(@RegularExpression("[0-9]{4}") val fourDigits: String)
data class RegexExampleResponse(val ok: String)
