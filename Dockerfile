FROM openjdk:11-jre

ADD target/ca-cmc-admin-client-1.0.3.jar /app.jar
ENTRYPOINT ["java","-jar","/app.jar"]

# Main web port
EXPOSE 8080
# HTTPS web port
EXPOSE 8443
# AJP port
EXPOSE 8009
