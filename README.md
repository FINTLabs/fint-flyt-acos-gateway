# FINT Flyt ACOS Gateway

Spring Boot Web service that ingests ACOS form submissions into FINT Flyt, persists binary payloads via the Flyt
file service, hands structured content to the shared `InstanceProcessor`, and exposes a companion endpoint so clients
can follow a submission’s progress inside the target archive system. It runs as a blocking OAuth2-protected gateway,
keeps key code lists cached via Kafka, and reuses Flyt web-instance-gateway components for integration lookup,
validation, and archiving.

## Highlights

- **External ACOS ingress** — Spring MVC controller under `/api/external/acos/instances` receives ACOS payloads and
  hands
  them through the shared `InstanceProcessor` so integration-specific flows (routing, validation, archival) remain
  centralized.
- **Instance-to-archive mapping** — `AcosInstanceMapper` converts elements into key/value maps, persists the rendered
  PDF + attachments through the Flyt file service, and returns `InstanceObject` graphs compatible with the downstream
  archive adapters.
- **Case insight endpoint** — `ArchiveCaseService` + `CaseInfoMappingService` expose `/case-info` so ACOS can display
  archive case IDs, managers, administrative units, and statuses derived from cached FINT resources.
- **Kafka-backed code list caches** — Dedicated consumer containers populate `FintCache` instances for
  `AdministrativEnhet`, `Arkivressurs`, `Saksstatus`, `Personalressurs`, and `Person` resources, ensuring case info
  enrichment works offline from upstream APIs.
- **Payload safeguards** — Bean Validation checks for non-empty metadata, base64-encoded blobs, and unique element IDs (
  `@UniqueElementIds`), while the resource server restricts calls to authorized source application IDs.

## Architecture Overview

| Component                                                                   | Responsibility                                                                                                                                                                      |
|-----------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `AcosInstanceController`                                                    | Hosts POST + GET endpoints, logs requests, resolves authenticated principals, and delegates to the instance processor or archive case service.                                      |
| `InstanceProcessorConfiguration`                                            | Builds an `InstanceProcessor<AcosInstance>` via `InstanceProcessorFactoryService`, wiring metadata extractors (formId + instanceId) and the mapper.                                 |
| `AcosInstanceMapper`                                                        | Turns ACOS elements/documents into Flyt `InstanceObject`s, uploads PDFs/attachments with a provided `persistFile` function, and injects generated file IDs into the instance graph. |
| `CaseInfoMappingService`                                                    | Converts `SakResource` objects into lightweight `CaseInfo` DTOs using cached status, administrative unit, personnel, and person resources.                                          |
| `ResourceEntityCacheConfiguration` & `ResourceEntityConsumersConfiguration` | Provision EHCache-backed `FintCache`s and Kafka request/reply consumers that refresh code lists whenever new resources arrive.                                                      |
| `ResourceLinkUtil` & `NoSuchLinkException`                                  | Helper utilities for resolving `Link` relations from FINT resources with consistent error handling.                                                                                 |
| `UniqueElementIds` / `UniqueElementIdsValidator`                            | Custom Jakarta Bean Validation constraint that reports duplicate element IDs and feeds offending keys back through Hibernate Validator payloads.                                    |
| `External profiles & configuration`                                         | `application.yaml` auto-includes Flyt Kafka, logging, web-resource-server, and file-client profiles so the gateway boots with the standard Flyt infrastructure defaults.            |

## HTTP API

Base path: `/api/external/acos/instances`

| Method | Path                                       | Description                                                                                                                                                                         | Request body                     | Response                                                                                                                                                                  |
|--------|--------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `POST` | `/`                                        | Receives an ACOS instance, validates metadata + base64 sections, persists files, and forwards the resulting `InstanceObject` to the configured integration via `InstanceProcessor`. | `AcosInstance` JSON (see below). | `202 Accepted` when the processor takes ownership, or integration-specific error codes (e.g., validation, no integration, deactivated) surfaced from `InstanceProcessor`. |
| `GET`  | `/{sourceApplicationInstanceId}/case-info` | Looks up the archive case tied to the ACOS instance ID, enriches it with manager, administrative unit, and status details, and returns a `CaseInfo` snapshot.                       | –                                | `200 OK` with `CaseInfo`, `404` when no case is found, `401/403` on auth failures.                                                                                        |

Example `AcosInstance` payload:

```json
{
  "metadata": {
    "formId": "TEST0488",
    "instanceId": "100384",
    "instanceUri": "https://acos.example.com/instance/100384"
  },
  "elements": [
    {
      "id": "Fornavn",
      "value": "Ola"
    },
    {
      "id": "Etternavn",
      "value": "Nordmann"
    }
  ],
  "formPdfBase64": "JVBERi0xLjQKJc...",
  "documents": [
    {
      "name": "vedlegg-bilde.jpg",
      "type": "image/jpeg",
      "encoding": "UTF-8",
      "base64": "/9j/4AAQSkZJRgABAQAAAQABAAD..."
    }
  ]
}
```

Example CaseInfo response:

```json
{
  "instanceId": "100384",
  "archiveCaseId": "23/12345",
  "caseManager": {
    "firstName": "Kari",
    "lastName": "Nordmann",
    "email": "kari.nordmann@example.no",
    "phone": "90000000"
  },
  "administrativeUnit": {
    "name": "Oppvekst"
  },
  "status": {
    "name": "Under behandling",
    "code": "B"
  }
}
```

Validation failures surface as 400 Bad Request with Bean Validation messages; missing cases return 404 Not Found.

## Kafka Integration

- ResourceEntityConsumersConfiguration registers five request/reply containers (administrative units, archive resources,
  case statuses, personnel, and persons). Each listener stores resources in the matching FintCache, keyed by self-link,
  so later lookups avoid live API calls.
- Topic names follow Flyt conventions via EntityTopicNameParameters + TopicNamePrefixParameters, inheriting org ID +
  domain-context from configuration.
- InstanceProcessor (from no.novari:flyt-web-instance-gateway) performs the heavy lifting: fetching integration
  metadata
  over Kafka, invoking validators, managing file persistence, and publishing archive-ready instances.
- Error handling relies on ErrorHandlerFactory configured with no retries + skip-failed semantics, preventing poison
  records from blocking cache refresh.

## Scheduled Tasks

The gateway runs entirely event-driven. No scheduled jobs or cron tasks are defined; all work happens within request
handling or Kafka message processing.

## Configuration

Spring profiles automatically include flyt-kafka, flyt-logging, flyt-web-resource-server, and flyt-file-client. A
local-staging profile overrides connectivity for local development.

| Property                                                                    | Description                                                                                                      |
|-----------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------|
| fint.application-id                                                         | Shared application identifier that scopes Kafka consumer groups and topic prefixes.                              |
| novari.flyt.web-resource-server.security.api.external.enabled               | Enables the external API exposure; complement with authorized-source-application-ids to restrict clients.        |
| novari.flyt.file-service-url                                                | Base URL for Flyt file-service uploads (defaults to the cluster service; overridden locally).                    |
| spring.security.oauth2.resourceserver.jwt.issuer-uri                        | Identity provider used to validate inbound OAuth2 JWTs (default: https://idp.felleskomponent.no/nidp/oauth/nam). |
| spring.security.oauth2.client.registration.file-service.*                   | Client credentials for the file-service OAuth client (injected via Kubernetes secrets).                          |
| spring.kafka.bootstrap-servers & novari.kafka.*                             | Kafka connectivity, default replicas, topic org IDs, and domain context.                                         |
| novari.flyt.web-instance-gateway.check-integration-exists                   | Local override that disables integration existence checks (handy for development).                               |
| server.max-http-request-header-size                                         | Increases request header limits to accommodate large ACOS payloads.                                              |
| server.servlet.context-path                                                 | Set per overlay so servlet routes, actuator endpoints, and ingress paths stay aligned.                           |
| logging.level.*                                                             | Enables detailed client/debug logging during development.                                                        |

Secrets referenced by manifests must supply OAuth client IDs/secrets (fint.flyt.acos.sso.*, fint.sso.*) and any archive
credentials required by downstream services.

## Running Locally

Prerequisites:

- Java 25+
- Docker (for Kafka + supporting services) or access to an existing dev cluster
- Local Flyt file-service and archive dependencies (or mocked endpoints) if you need end-to-end flows

Helpful commands:
```shell
./gradlew clean build # compile sources + run tests
./gradlew test # execute unit tests (InstanceMapper, validators, etc.)
SPRING_PROFILES_ACTIVE=local-staging \
./gradlew bootRun # start the service with local ports and unsecured Kafka
```

The local-staging profile expects Kafka on localhost:9092, file-service at http://localhost:8091, and binds HTTP to 
port 8101. Supply OAuth client IDs/secrets through environment variables or a local .envrc. When running locally,
authenticate requests with a JWT issued by the configured IdP or disable the web-resource-server guard only for isolated
testing.

## Deployment

Kustomize layout:

- kustomize/base/ — canonical Application manifest (flais.yaml) plus two NamOAuthClientApplicationResource definitions
  for the ACOS and file-service OAuth clients.
- kustomize/overlays/<org>/<env>/ — add environment-specific patches here (namespace, ingress path, Kafka ACLs,
  servlet context path, scaling). Overlays can re-point env, envFrom, or image tags without touching the base.

Update the rendered manifests whenever you change configuration defaults (image names, env vars, OAuth clients). CI/CD
pipelines typically point Kustomize directly at the appropriate overlay.

## Security

- OAuth2 web-resource-server validates JWTs issued by idp.felleskomponent.no; unauthorized or expired tokens receive
  401.
- External API access is further restricted by
  novari.flyt.web-resource-server.security.api.external.authorized-source-application-ids, ensuring only registered ACOS
  source application IDs can post instances or fetch case info.
- Outbound calls to the Flyt file service use the dedicated OAuth2 client defined in application-flyt-file-client.yaml.
  Separate NamOAuthClientApplicationResource objects provision the necessary credentials in Kubernetes.
- InstanceProcessor surfaces domain-specific error codes from ErrorCode, enabling clients to distinguish between
  validation failures, missing integrations, and system errors.

## Observability & Operations

- Liveness/readiness: GET /actuator/health
- Prometheus metrics: GET /actuator/prometheus
- Structured logging: inherits Flyt logging profile (JSON-friendly logs, correlation IDs).
- Max request size + header limits are tuned for large ACOS payloads; monitor heap usage and adjust JAVA_TOOL_OPTIONS as
  needed.

## Development Tips

- When extending mapping logic, update AcosInstanceMapper and cover new behavior in InstanceMapperTest; the current test
  suite asserts PDF + attachment persistence and key generation.
- Use CaseInfoMappingService as the single place for archive-to-DTO transformations so cache lookups and null-handling
  stay consistent.
- The Kafka caches rely on link relations; if you add new enriched fields, ensure the upstream FintCache receives the
  linked resource and update ResourceLinkUtil helpers if new link types appear.
- Bean Validation (@UniqueElementIds, @ValidBase64) only triggers when payloads are annotated with @Valid. Keep that in
  mind if you introduce new controllers or DTOs.
- Flyway migrations live under src/main/resources/db/migration when database changes are needed (currently unused but
  follows Flyt conventions).

## Contributing

1. Create a feature branch for your change.
2. Run ./gradlew test (and any relevant integration checks) before opening a PR.
3. Update Kustomize manifests if you touch deployment-related configuration.
4. Add or adjust unit/integration tests that cover the behavior you change or introduce.

———

FINT Flyt ACOS Gateway is maintained by the FINT Flyt team. Reach out through the internal Slack channel or
open an issue in this repository if you have questions or feature requests.
