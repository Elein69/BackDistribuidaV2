# Etapa de compilación
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

# Copia todos los archivos del proyecto
COPY pom.xml .
COPY src ./src

# Compila el JAR ejecutable con dependencias
RUN mvn clean package -DskipTests

# Etapa de ejecución
FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

# Copia el JAR desde la etapa de compilación
COPY --from=build /app/target/AgendaSimpleBackend-1.0.jar app.jar

# Expone el puerto si es necesario (ajusta si tu servidor usa otro)
EXPOSE 8080

# Comando para ejecutar la app
CMD ["java", "-jar", "app.jar"]

