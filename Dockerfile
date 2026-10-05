# Imagen única para los dos servicios (pedidos e inventario).
# El servicio que arranca se elige con la variable SPRING_PROFILES_ACTIVE en docker-compose.yaml.

# ---------- Etapa 1: compilar con Maven ----------
FROM maven:3.9.16-eclipse-temurin-21-noble AS build
WORKDIR /app

# Primero solo el pom.xml: si no cambian las dependencias, Docker reutiliza esta capa en caché.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Luego el código fuente. Las pruebas ya corren en el pipeline de CI (mvn verify).
COPY src ./src
RUN mvn -B -q package -DskipTests

# ---------- Etapa 2: imagen de ejecución (solo JRE) ----------
FROM eclipse-temurin:21-jre-noble

# Usuario sin privilegios: el proceso no corre como root dentro del contenedor.
RUN groupadd --system tienda && useradd --system --gid tienda --no-create-home tienda

WORKDIR /app
COPY --from=build /app/target/tienda.jar app.jar
USER tienda

# 8081 = pedidos, 8082 = inventario
EXPOSE 8081 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
