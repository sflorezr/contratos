# Etapa 1: Construir el proyecto con Gradle
FROM gradle:8.4.0-jdk17-alpine AS build
COPY --chown=gradle:gradle . /app
WORKDIR /app
RUN gradle build -x test

# Etapa 2: Imagen ligera para ejecutar la app
FROM eclipse-temurin:17-jdk-alpine
VOLUME /tmp
COPY --from=build /app/build/libs/app-1.0.0-boot.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]