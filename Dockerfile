FROM eclipse-temurin:21
COPY build/libs/aims-bulk-service-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]