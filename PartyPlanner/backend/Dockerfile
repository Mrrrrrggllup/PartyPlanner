# ── Build stage ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Copie le wrapper Gradle en premier (utilise la version exacte du projet)
COPY gradlew gradlew
COPY gradle/ gradle/
RUN chmod +x gradlew

# Copie les fichiers de config Gradle
COPY build.gradle.kts settings.gradle.kts gradle.properties* ./
COPY backend/build.gradle.kts backend/build.gradle.kts
COPY shared/build.gradle.kts shared/build.gradle.kts
COPY composeApp/build.gradle.kts composeApp/build.gradle.kts

# Télécharge les dépendances (layer mis en cache si les .kts ne changent pas)
RUN ./gradlew :backend:dependencies --no-daemon || true

# Copie le code source
COPY backend/src backend/src
COPY shared/src shared/src

# Build le fat JAR
RUN ./gradlew :backend:buildFatJar --no-daemon

# ── Runtime stage ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /app/backend/build/libs/backend-all.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]