# Multi-stage Dockerfile for Spring Boot app
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
COPY src ./src
RUN sed -n '1,200p' pom.xml >/dev/null || true
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:17-jre-jammy
ARG JAR_FILE=target/hospital-management-system-1.0.0.jar
COPY --from=build /workspace/${JAR_FILE} app.jar
ENV JAVA_OPTS="-Xms256m -Xmx1024m"
EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app.jar"]
