# Carabassa — Agent Instructions

## Project
Application with a Spring Boot backend and CLI, and Vue 3 frontend.

## Build the project

```bash
mvn clean install
```

## Run application

### Run the backend
```bash
cd server/boot
export CARABASSA_REPO_DIR=/tmp/carabassa && mvn spring-boot:run
```
Starts at http://localhost:8080

### Run the frontend
```bash
cd server/frontend/src/main/js/frontend
yarn install
yarn dev
```
Starts at http://localhost:3000

### Run the cli
```bash
cd cli
mvn spring-boot:run
```

## Run the tests

### Run the backend tests
```bash
cd server/boot && mvn test
```

### Run the frontend tests
```bash
cd server/frontend/src/main/js/frontend && yarn test
```

### Run the cli tests
```bash
cd cli && mvn test
```

### Run all the tests
```bash
mvn test
```

## Prepare minimal environment for manual testing

1. Run the backend.
2. Run the frontend.
3. Create a dataset:
```bash
export CARABASSA_API_URL=http://localhost:8080/api
cd cli
mvn spring-boot:run -Dspring-boot.run.arguments="create --dataset=test"
mvn spring-boot:run -Dspring-boot.run.arguments="upload --dataset=test --path=../engine/indexer/rdbms/src/test/resources/images"
mvn spring-boot:run -Dspring-boot.run.arguments="upload --dataset=test --path=../engine/indexer/rdbms/src/test/resources/videos"
```

## Important notes
