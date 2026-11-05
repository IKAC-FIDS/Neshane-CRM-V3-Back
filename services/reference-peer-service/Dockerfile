FROM eclipse-temurin:21-jre-jammy@sha256:1eac8b8fee11ca7d6725a48bc5d41992ed1561504ebae25490d2d062847e8269

WORKDIR /opt/neshane

COPY --chown=10001:10001 build/libs/reference-peer-service.jar /opt/neshane/application.jar

USER 10001:10001

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/opt/neshane/application.jar"]
