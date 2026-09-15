FROM eclipse-temurin:25-jdk AS build
WORKDIR /build

RUN apt-get update && apt-get install -y --no-install-recommends curl unzip && rm -rf /var/lib/apt/lists/*

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN ./mvnw -B dependency:go-offline

COPY src src
RUN ./mvnw -B package -DskipTests

FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app

COPY --from=build /build/target/quarkus-app/lib/ /app/lib/
COPY --from=build /build/target/quarkus-app/*.jar /app/
COPY --from=build /build/target/quarkus-app/app/ /app/app/
COPY --from=build /build/target/quarkus-app/quarkus/ /app/quarkus/

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "quarkus-run.jar"]
