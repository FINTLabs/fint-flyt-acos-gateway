package no.novari.flyt.acos.instance.gateway

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor
import org.springframework.web.client.RestClient
import java.time.Duration

@Configuration
class ArchiveGatewayClientConfiguration {
    // Uses the token manager from flyt-gateway-starter, so the token request has the same timeouts as the other
    // internal clients.
    @Bean
    fun archiveGatewayRestClient(
        restClientBuilder: RestClient.Builder,
        @Qualifier("internalClientAuthorizedClientManager") authorizedClientManager: OAuth2AuthorizedClientManager,
        requestFactoryBuilder: ClientHttpRequestFactoryBuilder<*>,
        @Value(
            "\${novari.flyt.archive-gateway-url:http://fint-flyt-archive-gateway:8080\${server.servlet.context-path:}}",
        )
        baseUrl: String,
    ): RestClient {
        val interceptor = OAuth2ClientHttpRequestInterceptor(authorizedClientManager)
        interceptor.setClientRegistrationIdResolver { CLIENT_REGISTRATION_ID }
        return restClientBuilder
            .requestFactory(
                requestFactoryBuilder.build(
                    ClientHttpRequestFactorySettings.defaults().withTimeouts(CONNECT_TIMEOUT, READ_TIMEOUT),
                ),
            ).requestInterceptor(interceptor)
            .baseUrl("${baseUrl.trimEnd('/')}/api/intern-klient")
            .build()
    }

    private companion object {
        const val CLIENT_REGISTRATION_ID = "archive-gateway"
        val CONNECT_TIMEOUT: Duration = Duration.ofSeconds(2)

        // archive-gateway looks the case up in FINT. Same limit as the Kafka reply timeout this replaces.
        val READ_TIMEOUT: Duration = Duration.ofSeconds(60)
    }
}
