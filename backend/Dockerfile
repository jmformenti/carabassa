FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app
COPY . /app

RUN mvn -pl backend/server/boot,cli -am package

FROM eclipse-temurin:21-jre

ENV CARABASSA_REPO_DIR=/data
ENV CARABASSA_API_URL=http://localhost:8080/api

WORKDIR /app

COPY --from=build /app/backend/server/boot/target/carabassa-boot-*.jar /app/app.jar
COPY --from=build /app/cli/target/carabassa-cli-*.jar /app/carabassa-cli.jar

RUN printf '%s\n' \
  '#!/bin/sh' \
  'exec java -jar /app/carabassa-cli.jar "$@"' \
  > /usr/local/bin/carabassa \
  && chmod +x /usr/local/bin/carabassa

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
