# Use the official Maven image with JDK
# FROM maven:3.8.4-openjdk-17
FROM maven:3.9.9-amazoncorretto-8-debian-bookworm

RUN apt-get update && apt-get install -y iputils-ping curl

# Set the working directory inside the container
WORKDIR /app

# Copy the source code to the container
COPY . .

# Build the application and skip tests for faster build (optional)
RUN mvn clean package -DskipTests

EXPOSE 9000

ENV SPRING_PROFILES_ACTIVE=noinit

# Add this line after the existing `COPY` instructions
COPY wait-for-it.sh /wait-for-it.sh

# Give execution permission to the script
RUN chmod +x /wait-for-it.sh

# # Run the Maven verify command to test the build
# CMD ["mvn", "spring-boot:run","-Dspring.profiles.active=noinit"]
# Keep the container running
CMD ["tail", "-f", "/dev/null"]