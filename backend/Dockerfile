FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /workspace

COPY pom.xml ./
RUN mvn -B -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends netcat-openbsd wget \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY docker/wait-for-services.sh /wait-for-services.sh
COPY --from=builder /workspace/target/*.jar /app/app.jar

RUN mkdir -p /app/uploads \
    && chmod +x /wait-for-services.sh

EXPOSE 8080

ENTRYPOINT ["/wait-for-services.sh"]
CMD ["java", "-jar", "/app/app.jar"]
