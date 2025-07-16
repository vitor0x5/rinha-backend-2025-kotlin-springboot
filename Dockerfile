FROM eclipse-temurin:21-alpine
WORKDIR /app
COPY /build/libs/*.jar app.jar
EXPOSE 9999
ENTRYPOINT ["java", "-jar", "app.jar"]