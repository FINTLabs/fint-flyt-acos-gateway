package no.novari.flyt.acos.instance.gateway

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.sun.net.httpserver.HttpServer
import no.novari.flyt.gateway.instance.FileClient
import no.novari.flyt.gateway.instance.kafka.ReceivedInstanceEventProducerService
import no.novari.flyt.gateway.instance.model.File
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.net.InetSocketAddress
import java.time.Instant
import java.util.UUID

@SpringBootTest(
    properties = [
        "fint.org-id=fintlabs.no",
        "novari.kafka.topic.org-id=fintlabs-no",
        "spring.kafka.listener.auto-startup=false",
        "spring.kafka.admin.auto-create=false",
        "novari.flyt.gateway-starter.check-integration-exists=false",
        "novari.flyt.security.external.expected-client-id=acos-client",
        "fint.flyt.authorization.sso.client-id=internal-client",
        "fint.flyt.authorization.sso.client-secret=test",
        "fint.sso.client-id=file-client",
        "fint.sso.client-secret=test",
    ],
)
@AutoConfigureMockMvc
class AcosSecurityHttpTest {
    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var listeners: KafkaListenerEndpointRegistry

    @MockitoBean
    private lateinit var fileClient: FileClient

    @MockitoBean
    private lateinit var receivedInstances: ReceivedInstanceEventProducerService

    @Test
    fun `Acos client submits a valid instance through the real processor`() {
        whenever(fileClient.postFile(any<File>())).thenReturn(UUID.randomUUID())
        submit(token("acos-client"), 202)
        verify(fileClient).postFile(any<File>())
        verify(receivedInstances).publish(argThat { sourceApplicationId == 1L }, any())
    }

    @Test
    fun `internal client cannot submit Acos instances`() {
        submit(token("internal-client"), 401)
        verifyNoInteractions(fileClient, receivedInstances)
    }

    @Test
    fun `another client cannot submit Acos instances`() {
        submit(token("another-client"), 401)
        verifyNoInteractions(fileClient, receivedInstances)
    }

    @Test
    fun `invalid signature cannot submit Acos instances`() {
        val otherKey = RSAKeyGenerator(2048).keyID(key.keyID).generate()
        submit(token("acos-client", NimbusJwtEncoder(ImmutableJWKSet(JWKSet(otherKey)))), 401)
        verifyNoInteractions(fileClient, receivedInstances)
    }

    @Test
    fun `cache listeners preserve all topic names and the application group ID`() {
        val containers = listeners.listenerContainers
        assertThat(containers).hasSize(5)
        assertThat(containers.map { it.groupId }).containsOnly("fint-flyt-acos-gateway")
        assertThat(containers.flatMap { it.containerProperties.topics!!.toList() }).containsExactlyInAnyOrder(
            "fintlabs-no.flyt.entity.arkiv-noark-administrativenhet",
            "fintlabs-no.flyt.entity.arkiv-noark-arkivressurs",
            "fintlabs-no.flyt.entity.arkiv-kodeverk-saksstatus",
            "fintlabs-no.flyt.entity.administrasjon-personal-personalressurs",
            "fintlabs-no.flyt.entity.administrasjon-personal-person",
        )
    }

    private fun submit(
        token: String,
        expectedStatus: Int,
    ) {
        mvc
            .perform(
                post("/api/acos/instances")
                    .header("Authorization", "Bearer $token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"metadata":{"formId":"pilot","instanceId":"pilot-1"},
                        "elements":[{"id":"name","value":"Test"}],"formPdfBase64":"JVBERi0xLjQ="}""",
                    ),
            ).andExpect(status().`is`(expectedStatus))
    }

    companion object {
        private const val ISSUER = "https://idp.test/nam"
        private val key = RSAKeyGenerator(2048).keyID("acos-test").generate()
        private val encoder = NimbusJwtEncoder(ImmutableJWKSet(JWKSet(key)))
        private val jwks =
            HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
                createContext("/jwks") { exchange ->
                    val body = JWKSet(key.toPublicJWK()).toString().toByteArray()
                    exchange.responseHeaders.add("Content-Type", "application/json")
                    exchange.sendResponseHeaders(200, body.size.toLong())
                    exchange.responseBody.use { it.write(body) }
                }
                start()
            }

        @JvmStatic
        @DynamicPropertySource
        fun jwtProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri") { ISSUER }
            registry.add(
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
            ) { "http://127.0.0.1:${jwks.address.port}/jwks" }
        }

        private fun token(
            clientId: String,
            signer: NimbusJwtEncoder = encoder,
        ): String {
            val now = Instant.now()
            val claims =
                JwtClaimsSet
                    .builder()
                    .issuer(ISSUER)
                    .subject(clientId)
                    .audience(listOf(clientId))
                    .issuedAt(now)
                    .expiresAt(now.plusSeconds(300))
                    .claim("scope", "end-user-profile fint-api")
                    .build()
            return signer
                .encode(
                    JwtEncoderParameters.from(
                        JwsHeader.with(SignatureAlgorithm.RS256).keyId(key.keyID).build(),
                        claims,
                    ),
                ).tokenValue
        }
    }
}
