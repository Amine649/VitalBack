# Use a stable JDK base image
FROM eclipse-temurin:21-jdk

# Set working directory
WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Make mvnw executable
RUN chmod +x mvnw

# Pre-download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build and show what was created
RUN ./mvnw clean package -DskipTests && \
    echo "=== JAR built successfully ===" && \
    ls -la target/*.jar

# Expose app port
EXPOSE 8080

# Run the JAR (dynamically)
CMD ["sh", "-c", "exec java -jar target/*.jar"]