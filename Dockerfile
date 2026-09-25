# Use an official OpenJDK runtime as a parent image
FROM openjdk:11-jdk-slim

# Set the working directory in the container
WORKDIR /app

# Copy the current directory contents into the container at /app
COPY . /app

# Set working directory to the backend for compilation
WORKDIR /app/backend

# Compile the Java code
RUN javac *.java

# Run the application
CMD ["java", "WebServer"]
