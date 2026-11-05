FROM eclipse-temurin:21-jdk AS compilacao
WORKDIR /projeto
COPY gradle gradle
COPY gradlew build.gradle.kts settings.gradle.kts gradle.properties ./
RUN chmod +x gradlew
COPY src src
RUN ./gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 monoco && mkdir logs && chown monoco logs
COPY --from=compilacao /projeto/build/libs/monoco-backend-0.3.0.jar app.jar
USER monoco
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
