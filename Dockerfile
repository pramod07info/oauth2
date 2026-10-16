FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src src
RUN mvn -B -DskipTests package
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --gid 10001 app && useradd --uid 10001 --gid app app
COPY --from=build /build/target/oauth2-1.0.0-SNAPSHOT.jar app.jar
USER 10001:10001
EXPOSE 9000
ENTRYPOINT ["java","-jar","/app/app.jar"]
