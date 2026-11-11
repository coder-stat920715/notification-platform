# Build one service image:  docker build --build-arg MODULE=dispatcher-service -t notify/dispatcher .
FROM maven:3.9-eclipse-temurin-17 AS build
ARG MODULE
WORKDIR /src
COPY . .
RUN mvn -q -pl ${MODULE} -am -DskipTests package

FROM eclipse-temurin:17-jre
ARG MODULE
WORKDIR /app
COPY --from=build /src/${MODULE}/target/${MODULE}-1.0.0.jar app.jar
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
