FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .

RUN chmod +x gradlew

COPY src src

RUN ./gradlew bootJar --no-daemon


FROM eclipse-temurin:21-jre

WORKDIR /app

RUN useradd --system --uid 10001 signaldesk

COPY --from=build /workspace/build/libs/*.jar app.jar

USER signaldesk

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
