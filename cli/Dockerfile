FROM eclipse-temurin:21-jre

ENV CARABASSA_API_URL=http://localhost:8080/api

WORKDIR /app

ARG JAR_FILE=target/carabassa-cli-*.jar
COPY ${JAR_FILE} /app/carabassa-cli.jar

RUN printf '%s\n' \
  '#!/bin/sh' \
  'exec java -jar /app/carabassa-cli.jar "$@"' \
  > /usr/local/bin/carabassa \
  && chmod +x /usr/local/bin/carabassa

ENTRYPOINT ["/usr/local/bin/carabassa"]
