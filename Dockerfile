# Build stage
FROM gradle:8.14-jdk21-alpine AS build
WORKDIR /app

# Copiar arquivos do Gradle
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle gradle
COPY gradlew gradlew.bat ./

# Copiar código fonte
COPY src ./src

# Build da aplicação (sem rodar testes para ser mais rápido)
RUN gradle build -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copiar apenas o JAR compilado
COPY --from=build /app/build/libs/*.jar app.jar

# Expor porta 8080
EXPOSE 8080

# Comando para rodar a aplicação
ENTRYPOINT ["java", "-jar", "app.jar"]

