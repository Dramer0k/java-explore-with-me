FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY stats/stat_srv/target/stat_srv-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "app.jar"]