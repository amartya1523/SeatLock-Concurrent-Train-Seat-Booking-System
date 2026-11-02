FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre-jammy
RUN groupadd --system seatlock && useradd --system --gid seatlock seatlock
WORKDIR /app
COPY --from=build /app/target/seatlock-1.0.0.jar app.jar
USER seatlock
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=25s --retries=5 CMD bash -c 'exec 3<>/dev/tcp/localhost/8080' || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
