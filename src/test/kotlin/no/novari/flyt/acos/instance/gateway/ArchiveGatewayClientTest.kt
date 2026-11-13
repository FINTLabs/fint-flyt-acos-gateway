package no.novari.flyt.acos.instance.gateway

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestClient
import java.time.Instant

class ArchiveGatewayClientTest {
    private val baseUrl = "http://fint-flyt-archive-gateway:8080/beta/afk-no/api/intern-klient"
    private val restClientBuilder =
        ArchiveGatewayClientConfiguration()
            .archiveGatewayRestClient(
                RestClient.builder(),
                tokenPerRegistration(),
                ClientHttpRequestFactoryBuilder.detect(),
                "http://fint-flyt-archive-gateway:8080/beta/afk-no",
            ).mutate()
    private val server = MockRestServiceServer.bindTo(restClientBuilder).build()
    private val client = ArchiveGatewayClient(restClientBuilder.build())

    @AfterEach
    fun verify() {
        server.verify()
    }

    @Test
    fun `sends archive-gateway token and returns the sak`() {
        server
            .expect(requestTo("$baseUrl/arkiv/saker?archiveInstanceId=2024%2F123-%5B1%2C2%5D"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token-archive-gateway"))
            .andRespond(withSuccess("""{"mappeId":{"identifikatorverdi":"2024/123"}}""", MediaType.APPLICATION_JSON))

        val sak = client.getCaseWithFilteredJournalposts("2024/123-[1,2]")

        assertThat(sak?.mappeId?.identifikatorverdi).isEqualTo("2024/123")
    }

    @Test
    fun `404 returns null like the Kafka reply did`() {
        server
            .expect(requestTo("$baseUrl/arkiv/saker?archiveInstanceId=2024%2F123"))
            .andRespond(withStatus(HttpStatus.NOT_FOUND))

        assertThat(client.getCaseWithFilteredJournalposts("2024/123")).isNull()
    }

    @Test
    fun `server error is thrown`() {
        server
            .expect(requestTo("$baseUrl/arkiv/saker?archiveInstanceId=2024%2F123"))
            .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE))

        assertThrows<HttpServerErrorException> { client.getCaseWithFilteredJournalposts("2024/123") }
    }

    private fun tokenPerRegistration() =
        OAuth2AuthorizedClientManager { request ->
            OAuth2AuthorizedClient(
                ClientRegistration
                    .withRegistrationId(request.clientRegistrationId)
                    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                    .clientId("client")
                    .tokenUri("http://idp/token")
                    .build(),
                "anonymousUser",
                OAuth2AccessToken(
                    OAuth2AccessToken.TokenType.BEARER,
                    "token-${request.clientRegistrationId}",
                    Instant.now(),
                    Instant.now().plusSeconds(60),
                ),
            )
        }
}
