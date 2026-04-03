FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the built jar
COPY target/*.jar app.jar

# Copy the wait script into the image
COPY wait-for-keycloak.sh .

# Make it executable
RUN chmod +x wait-for-keycloak.sh

EXPOSE 8080

# Always start via the wait script
ENTRYPOINT ["./wait-for-keycloak.sh"]