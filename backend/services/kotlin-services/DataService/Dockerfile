# First stage: use official Gradle image to build the project
FROM gradle:8.13-jdk21 AS build

WORKDIR /app

COPY . .

RUN gradle build --no-daemon -x test

# Second stage: use a slim JDK image to run the app
FROM openjdk:21-jdk-slim

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
