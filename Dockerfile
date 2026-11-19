# syntax=docker/dockerfile:1

# --- build: kompilacja i testy (bez Javy/Mavena na hoście) ---
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

COPY mvnw pom.xml ./
COPY .mvn .mvn
# Na Windowsie mvnw może mieć CRLF; w kontenerze musi być LF.
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

COPY src src
# Testy chodzą na FakeWeatherProvider, więc nie potrzebują sieci. Cache ~/.m2 między buildami.
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q -B package \
    && cp target/pogodulka-*.jar app.jar

# --- runtime: sam JRE i jar ---
FROM eclipse-temurin:25-jre
WORKDIR /app

# "Dziś" w walidacji dat liczymy po polskim czasie, nie UTC.
ENV TZ=Europe/Warsaw

RUN useradd --system --no-create-home weather \
    && mkdir /app/data && chown weather /app/data
USER weather

# Log requestów dla panelu admina. Katalog montujemy jako wolumen (-v weather-data:/app/data), żeby przetrwał deploy.
ENV WEATHER_REQUEST_LOG_FILE=/app/data/requests.jsonl

COPY --from=build /app/app.jar app.jar

EXPOSE 8080
# Konfiguracja przez zmienne środowiskowe, np. -e WEATHER_PROVIDER=fake, -e SERVER_PORT=8081
ENTRYPOINT ["java", "-jar", "app.jar"]
