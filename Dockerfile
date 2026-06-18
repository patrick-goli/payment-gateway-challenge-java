# ---- Build ----
FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# Download dependencies
# CRLF issues on Windows
RUN sed -i 's/\r$//' gradlew
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon

# Copy the source
COPY src src

# Build the application
RUN ./gradlew bootJar --no-daemon

# ---- Run ----
FROM eclipse-temurin:17-jre AS runtime

RUN groupadd -r appgroup && useradd -r -g appgroup appuser

RUN mkdir -p /var/log/payment-gateway
RUN chown -R appuser:appgroup /var/log/payment-gateway

WORKDIR /app

# Copy the built jar
COPY --from=build /app/build/libs/*.jar app.jar

# Expose application port
EXPOSE 8090
ENV SERVER_PORT=8090
ENV LOGGING_FILE_NAME=/var/log/payment-gateway/application.log

USER appuser

ENTRYPOINT ["java","-jar","/app/app.jar"]