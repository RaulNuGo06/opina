# Una sola imagen con todo: Spring Boot sirve la API en /api y la app de Angular en el resto de rutas.

# 1. La app de Angular
FROM node:24-alpine AS frontend
WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npx ng build

# 2. La API, con la app compilada dentro como contenido estático
FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /app
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline
COPY src src
COPY --from=frontend /app/frontend/dist/frontend/browser src/main/resources/static
# Los tests ya los pasa la CI; aquí solo se empaqueta
RUN mvn -B -q package -DskipTests

# 3. Lo que se ejecuta: solo el JRE y el jar
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 opina
USER opina
COPY --from=backend /app/target/opina-*.jar app.jar
EXPOSE 8080
# Los servicios como Render dicen el puerto en PORT
ENTRYPOINT ["sh", "-c", "exec java -XX:MaxRAMPercentage=75 -Dserver.port=${PORT:-8080} -jar app.jar"]
