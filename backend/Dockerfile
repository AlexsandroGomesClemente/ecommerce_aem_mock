# ADR-001: Multi-stage Dockerfile para o Back-end Java (Spring Boot 3 + GraphQL + PostgreSQL)
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && apk add --no-cache curl tar && ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw clean package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/ecommerce-bff-backend-1.0.0.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseZGC", "-Xms256m", "-Xmx1024m", "-jar", "app.jar"]

