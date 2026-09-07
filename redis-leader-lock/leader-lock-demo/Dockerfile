FROM gradle:9.2.1-jdk25 AS build
WORKDIR /workspace
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
COPY leader-lock-library ./leader-lock-library
COPY leader-lock-demo ./leader-lock-demo
RUN gradle :leader-lock-demo:bootJar --no-daemon

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /workspace/leader-lock-demo/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]