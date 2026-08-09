FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY stats/stat_clt/target/stat_clt-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]