#!/bin/bash
# Load environment variables from .env file
export $(grep -v '^#' .env | xargs)

# Run the Spring Boot application
./mvnw spring-boot:run
