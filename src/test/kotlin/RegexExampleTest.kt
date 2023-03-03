import com.papsign.ktor.openapigen.annotations.type.string.pattern.RegularExpressionConstraintViolation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import org.junit.jupiter.api.assertThrows

class RegexExampleTest {
    @Test
    fun `test that regex validator works`(): Unit = testApplication {
//        application(testApplication {  })
        application(Application::regexExample)
        val client = createClient { install(ContentNegotiation) { jackson() } }

        assertThrows<RegularExpressionConstraintViolation> {
            client.post("/example") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(RegexExampleRequest(fourDigits = "1"))
            }
        }

        assertThrows<RegularExpressionConstraintViolation> {
            client.post("/example") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(RegexExampleRequest(fourDigits = "12345"))
            }
        }

        assertEquals(HttpStatusCode.OK, client.post("/example") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(RegexExampleRequest(fourDigits = "1234"))
        }.status)
    }
}
