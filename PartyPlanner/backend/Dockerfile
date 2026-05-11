# ── Build stage ──────────────────────────────────────────────────────────────
FROM gradle:8.5-jdk17 AS build
WORKDIR /app

# Copie uniquement les fichiers Gradle en premier (cache des dépendances)
COPY build.gradle.kts settings.gradle.kts gradle.properties* ./
COPY gradle/ gradle/
COPY backend/build.gradle.kts backend/build.gradle.kts
COPY shared/build.gradle.kts shared/build.gradle.kts
COPY composeApp/build.gradle.kts composeApp/build.gradle.kts

# Télécharge les dépendances (layer mis en cache si les .kts ne changent pas)
RUN gradle :backend:dependencies --no-daemon || true

# Copie le code source
COPY backend/src backend/src
COPY shared/src shared/src

# Build le fat JAR
RUN gradle :backend:buildFatJar --no-daemon

# ── Runtime stage ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /app/backend/build/libs/backend-all.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]