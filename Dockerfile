FROM openjdk:17-jdk-slim

ADD target/ca-cmc-admin-client-1.0.5-SNAPSHOT.jar /app.jar
ENTRYPOINT ["java","-jar","/app.jar"]

# Main web port
EXPOSE 8080
# HTTPS web port
EXPOSE 8443
# AJP port
EXPOSE 8009
