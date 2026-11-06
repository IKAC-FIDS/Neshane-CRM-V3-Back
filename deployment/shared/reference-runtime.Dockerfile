# V3-023 Stage B1 — disposable REFERENCE_ONLY runtime.
# Build context: contract-tests/routing/build/reference-gateway

FROM eclipse-temurin:21-jre-jammy@sha256:1eac8b8fee11ca7d6725a48bc5d41992ed1561504ebae25490d2d062847e8269

WORKDIR /opt/neshane

COPY --chown=10001:10001 classes/ /opt/neshane/classes/
COPY --chown=10001:10001 lib/ /opt/neshane/lib/

USER 10001:10001

EXPOSE 8080

ENTRYPOINT ["java", "-cp", "/opt/neshane/classes:/opt/neshane/lib/*"]
