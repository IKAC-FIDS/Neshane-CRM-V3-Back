# Reference Peer Service

`reference-peer-service` is the second, intentionally non-business Spring Boot reference service for `V3-017`.
It owns its executable artifact and container image and exposes only the versioned synthetic
`POST /api/v1/reference-echoes` contract. The service imports the narrow Observability platform library but no
other service implementation.

The endpoint accepts no tenant, identity, entitlement or authorization metadata and must not be interpreted as
an authorization precedent. It exists only to prove independent service builds, public-contract communication,
W3C trace/correlation propagation and safe validation behavior.

```powershell
.\gradlew.bat :services:reference-peer-service:clean :services:reference-peer-service:build --offline
$env:NESHANE_DEPLOYMENT_PROFILE='development'
$env:SERVER_PORT='8081'
java -jar services/reference-peer-service/build/libs/reference-peer-service.jar
```

Configuration is externalized through `SERVICE_NAME`, `SERVER_PORT`, `NESHANE_DEPLOYMENT_PROFILE` and the
existing V3-013 OpenTelemetry settings. No secret is required or accepted by this service.
