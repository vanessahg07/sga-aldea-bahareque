# ============================================================================
# Imagen del backend del SGA (API REST con Spring Boot) para Render.
# El frontend se despliega aparte en Vercel; la base de datos es PostgreSQL de Render.
# Para otra base basta con cambiar SPRING_PROFILES_ACTIVE y las variables SGA_BD_*.
# ============================================================================

# 1. Compilación (las pruebas se ejecutan en desarrollo, no al construir la imagen)
FROM eclipse-temurin:25-jdk AS compilacion
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null
COPY src src
RUN ./gradlew --no-daemon bootJar -x test

# 2. Ejecución con un usuario sin privilegios
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --create-home sga && mkdir -p /app/datos && chown sga /app/datos
COPY --from=compilacion /app/build/libs/*.jar sga.jar
USER sga
ENV SPRING_PROFILES_ACTIVE=postgres \
    SGA_DATOS=/app/datos \
    PORT=8080
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/sga.jar"]
