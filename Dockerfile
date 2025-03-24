FROM eclipse-temurin:21-jdk as build
WORKDIR /workspace/app

# Copy maven executable and config
COPY mvnw .
COPY .mvn .mvn 
COPY pom.xml . 
COPY src src

# Make Maven wrapper executable
RUN chmod +x ./mvnw

# Build the application
RUN ./mvnw package -DskipTests

# Create the runtime image
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/app/target/*.jar app.jar

# Environment variables
ENV AWS_REGION=us-east-1
ENV PORT=5000

EXPOSE 5000
ENTRYPOINT ["java", "-jar", "app.jar"]