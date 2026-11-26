# FINT Flyt ACOS Gateway

Receives ACOS submissions and form definitions, uploads attachments through file-service, and publishes FLYT
instance and integration metadata events. Case information is resolved through history-service and archive-gateway,
then enriched from five Kafka-backed code list caches.

Requires Java 25. Uses Spring Boot 4.1, Jackson 3, flyt-gateway-starter 6 and flyt-cache 4.

## HTTP API

All routes require a signed NAM machine token for this gateway's configured ACOS client. Other clients receive 401.
The security starter protects `/api/acos/**` before controller invocation; no additional `@PreAuthorize` is needed.

| Method | Path                                               | Success |
| ------ | -------------------------------------------------- | ------- |
| POST   | `/api/acos/instances`                              | 202     |
| POST   | `/api/acos/metadata`                               | 202     |
| GET    | `/api/acos/instances/{instanceId}/case-info`       | 200     |

Example submission:

```json
{
  "metadata": {"formId": "TEST0488", "instanceId": "100384"},
  "elements": [{"id": "Fornavn", "value": "Ola"}],
  "formPdfBase64": "JVBERi0xLjQ=",
  "documents": []
}
```

Repeated element IDs are joined with newlines. Invalid submissions return 400; the processor returns 422 for unknown
or deactivated integrations. Missing archive cases return 404.

## Configuration

`application.yaml` includes the Kafka, security, logging and OAuth client profiles. The source
application is `ACOS` (`novari.flyt.security.external.source-application`) and the external path is `/api/acos/**`.
The client ID `fint.flyt.acos.sso.client-id` comes from secret `fint-flyt-acos-oauth2-client`, same key.
Authorization-service owns the NAM declaration in afk-no, bfk-no and ofk-no (api and beta). All Acos overlays inherit
the secret reference from the base. Only the client ID is needed for inbound authorization.

Outbound registrations `intgr-conf-service`, `history-service` and `archive-gateway` use
`fint.flyt.authorization.sso.client-id` and `.client-secret`, mapped from the shared
`fint-flyt-authorization-oauth2-client` secret's `fint.sso.client-id` and `.client-secret` keys. `file-service` uses
`fint-flyt-file-oauth2-client` through `envFrom`. Internal HTTP lookups use client credentials.

Topic names remain `<org-id>.flyt.entity.<resource>` for:

- `arkiv-noark-administrativenhet`
- `arkiv-noark-arkivressurs`
- `arkiv-kodeverk-saksstatus`
- `administrasjon-personal-personalressurs`
- `administrasjon-personal-person`

All five listeners use `spring.kafka.consumer.group-id=${fint.application-id}` with no suffix. They seek to the
beginning on assignment to rebuild the in-memory caches, as before. Failed records are logged and skipped without
retries. Cache values are indexed by resource self-links; tombstones remove the Kafka key. Cache metrics are
registered automatically. The gateway, security, Kafka and cache starters use auto-configuration; component scanning
is restricted to Acos code.

Kafkarator must provide `spring.kafka.security.protocol=SSL` alongside the Kafka credentials. Local development uses
`docker-compose.yaml` and the `local-staging` profile; supply OAuth credentials and the expected ACOS client ID.

## Verification

```shell
./gradlew check
./gradlew dependencies --write-locks # after dependency changes
```

HTTP tests sign tokens with a test RSA key served over JWKS and use the real JwtDecoder, filter chains, mapper and
instance processor. File storage and event publication are mocked. See the shared
`docs/ffs-2328-spring-boot-4/ffs-2430-status.md` for pilot findings and the manual beta test procedure.
