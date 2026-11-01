FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd -r -u 10001 appuser
COPY --from=build /app/target/banking-operations-agent-0.1.0.jar /app/app.jar
USER appuser
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["sh","-c","exec java -Dserver.port=$PORT -jar /app/app.jar"]
