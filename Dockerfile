FROM maven:3.9.8-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn -q clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
COPY ca.pem /app/ca.pem

# Trust MySQL CA cert for VERIFY_CA / VERIFY_IDENTITY connections.
RUN keytool -importcert -file /app/ca.pem \
    -alias krishvas-db-ca \
    -keystore $JAVA_HOME/lib/security/cacerts \
    -storepass changeit -noprompt

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=$PORT"]
