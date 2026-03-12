# multi-stage build for Spring Boot application

# 1. build stage using Maven
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /workspace

# copy only the pom and download dependencies to leverage Docker cache
COPY pom.xml .
RUN mvn dependency:go-offline -B

# copy source code and package
COPY src ./src
RUN mvn clean package -DskipTests

# 2. runtime stage using a lightweight JRE image
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# prefer IPv4 stack so the container doesn’t try to open IPv6 sockets
ENV JAVA_TOOL_OPTIONS="-Djava.net.preferIPv4Stack=true"

# copy the fat jar/war produced by Spring Boot
# Change .war to .jar here
COPY --from=build /workspace/target/hospital-management-system-1.0.0.jar app.jar

# expose port (Cloud Run uses $PORT, but we expose 8080 for local runs)
EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]
