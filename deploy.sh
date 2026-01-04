#!/bin/bash
# Production deployment script

# Load environment variables
set -a
source .env
set +a

# Build the application
echo "Building application..."
./mvnw clean package -DskipTests

# Run with production profile
echo "Starting application in production mode..."
java -jar target/api-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
