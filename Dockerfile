# Usa una imagen base con Java y Maven
FROM maven:3.9.6-eclipse-temurin-17 AS build

# Crea y entra al directorio del proyecto
WORKDIR /app

# Copia el pom y descarga dependencias primero (caching)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copia el resto del proyecto y construye el JAR con dependencias (shade plugin)
COPY . .
RUN mvn package -DskipTests

# Fase final: usar una imagen base liviana con solo Java para ejecutar
FROM eclipse-temurin:17-jdk

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Cambia esto según tu clase principal
ENTRYPOINT ["java", "-jar", "app.jar"]

