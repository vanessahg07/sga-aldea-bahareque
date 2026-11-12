# Imagen única del SGA: compila el frontend (React) y el backend (Spring Boot) y los sirve
# desde el mismo servidor. La base de datos es H2 en archivo (perfil prod): cambiar a
# PostgreSQL es solo configuración (SPRING_PROFILES_ACTIVE=postgres y SGA_BD_URL).

# 1. Frontend
FROM node:24-alpine AS frontend
WORKDIR /frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# 2. Backend (las pruebas corren en el pipeline de desarrollo, no al construir la imagen)
FROM eclipse-temurin:25-jdk AS backend
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null
COPY src src
RUN ./gradlew --no-daemon bootJar -x test

# 3. Ejecución
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --create-home sga && mkdir -p /app/datos && chown sga /app/datos
COPY --from=backend /app/build/libs/*.jar sga.jar
COPY --from=frontend /frontend/dist /app/frontend
USER sga
ENV SPRING_PROFILES_ACTIVE=prod \
    SGA_FRONTEND_DIRECTORIO=/app/frontend \
    SGA_DATOS=/app/datos \
    PORT=8080
EXPOSE 8080
VOLUME /app/datos
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/sga.jar"]
