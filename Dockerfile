FROM eclipse-temurin:17-jdk AS build

WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw mvnw
COPY mvnw.cmd mvnw.cmd
COPY pom.xml pom.xml
COPY src src

RUN chmod +x mvnw && ./mvnw -DskipTests package

FROM eclipse-temurin:17-jre

WORKDIR /app
RUN mkdir -p /app/certs
COPY --from=build /workspace/target/bog-ide-price-scraper-0.0.1-SNAPSHOT.jar app.jar
COPY certs/bog-ide-price-scraper.p12 /app/certs/bog-ide-price-scraper.p12

EXPOSE 8443

ENTRYPOINT ["java", "-jar", "app.jar"]
