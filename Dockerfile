# Use the official Maven image with JDK
FROM maven:3.8.4-openjdk-17

# Set the working directory inside the container
WORKDIR /app

# Copy the source code to the container
COPY . .

# Build the application and skip tests for faster build (optional)
RUN mvn clean package -DskipTests

EXPOSE 9000

ENV SPRING_PROFILES_ACTIVE=noinit


# Give execution permission to the script
RUN chmod +x /wait-for-it.sh

# # Run the Maven verify command to test the build
# CMD ["mvn", "spring-boot:run","-Dspring.profiles.active=noinit"]