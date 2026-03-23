FROM eclipse-temurin:21-jre

ENV CARABASSA_REPO_DIR=/data

WORKDIR /app

ARG JAR_FILE=server/boot/target/carabassa-boot-*.jar
COPY ${JAR_FILE} /app/app.jar

RUN apt-get update && \
    apt-get install -y ffmpeg && \
    rm -rf /var/lib/apt/lists/*

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
